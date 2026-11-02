#!/usr/bin/env bash
# Builds the in-browser Java compiler used by the Lab page, from source:
#   teavm-javac (Apache-2.0, https://github.com/konsoletyper/teavm-javac) = OpenJDK 25 javac
#   (GPLv2 + Classpath Exception) + TeaVM, compiled to WebAssembly.
# The lab's class-library additions (java/teavm-patches: Scanner, InputMismatchException,
# JDK-compatible Formatter, keyboard input from the page) are added to both class libraries.
#
#   JAVA25_HOME=/path/to/jdk-25 bash tools/browser-javac/build.sh OUT_DIR
# Requires: git, JDK 25 (for this build only), the teavm-patches jar (mvn -f java/pom.xml install).
# Runs in GitHub Actions (see .github/workflows); takes 10-20 minutes the first time.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$(mkdir -p "$1" && cd "$1" && pwd)"
COMMIT=208d4c97a602ba0df055f16e6882059fb69d0122       # teavm-javac, 2026-10-05 (TeaVM 0.16.0)
JDK_REVISION=6c48f4ed707bf0b15f9b6098de30db8aae6fa40f  # openjdk/jdk25u revision used by that commit
PATCHES="$ROOT/java/teavm-patches/target/teavm-patches-1.0.0.jar"
[ -f "$PATCHES" ] || { echo "missing $PATCHES: run mvn -f java/pom.xml install first"; exit 1; }
[ -n "${JAVA25_HOME:-}" ] || { echo "set JAVA25_HOME to a JDK 25"; exit 1; }

WORK="${WORK:-$(mktemp -d)}"
if [ ! -d "$WORK/teavm-javac/.git" ]; then
  git clone --quiet https://github.com/konsoletyper/teavm-javac "$WORK/teavm-javac"
fi
cd "$WORK/teavm-javac"
git checkout --quiet "$COMMIT"
git reset --quiet --hard "$COMMIT"

# OpenJDK sources: fetched with git (the build would otherwise download a zip of the same revision)
if [ ! -f "javac/build/jdk-$JDK_REVISION.zip" ]; then
  mkdir -p javac/build "$WORK/jdk"
  ( cd "$WORK/jdk" && git init -q && (git remote add origin https://github.com/openjdk/jdk25u 2>/dev/null || true) \
      && git fetch -q --depth 1 origin "$JDK_REVISION" \
      && git archive --format=zip --prefix="jdk25u-$JDK_REVISION/" FETCH_HEAD -o "$WORK/teavm-javac/javac/build/jdk-$JDK_REVISION.zip" )
fi

# Add the lab's class-library additions.
#  - runtime class library (what student programs are linked with): every class of the jar; ours
#    replace TeaVM's where both exist (Formatter, Math) or add what TeaVM lacks (Scanner).
#  - compile-time API (what javac sees): only the classes TeaVM does not have, so that no class
#    appears twice; lab.web.BrowserStdin stays invisible to student code.
SDK_ADDITIONS="$WORK/sdk-additions.jar"
TEAVM_CLASSLIB="${TEAVM_CLASSLIB:-$HOME/.m2/repository/org/teavm/teavm-classlib/0.16.0/teavm-classlib-0.16.0.jar}"
[ -f "$TEAVM_CLASSLIB" ] || { echo "missing $TEAVM_CLASSLIB (mvn -f java/pom.xml install downloads it)"; exit 1; }
rm -rf "$WORK/sdk-additions" "$SDK_ADDITIONS"
mkdir -p "$WORK/sdk-additions"
# every class of the lab's additions that TeaVM does not have (java.sql, ObjectOutputStream, ...)
python3 - "$PATCHES" "$TEAVM_CLASSLIB" "$WORK/sdk-additions" <<'PY'
import os, sys, zipfile
patches, classlib, out = sys.argv[1:4]
existing = set(zipfile.ZipFile(classlib).namelist())
n = 0
with zipfile.ZipFile(patches) as z:
    for name in z.namelist():
        base = name.rsplit("/", 1)[-1]
        if name.startswith("org/teavm/classlib/") and name.endswith(".class") and base.startswith("T") \
                and name not in existing:
            target = os.path.join(out, name)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "wb") as f:
                f.write(z.read(name))
            n += 1
print("compile-time API additions:", n, "classes")
PY
( cd "$WORK/sdk-additions" && "$JAVA25_HOME/bin/jar" cf "$SDK_ADDITIONS" org )

# The run-time checks of the Java language (array index, division by zero, negative array size)
# are added to the class files javac produces, before TeaVM translates them (java/teavm-checks).
mkdir -p compiler/src/main/java/lab/teavm
cp "$ROOT/java/teavm-checks/src/main/java/lab/teavm/ChecksTransformer.java" compiler/src/main/java/lab/teavm/
python3 - compiler/src/main/java/org/teavm/javac/Compiler.java <<'PY'
import sys
path = sys.argv[1]
src = open(path, encoding="utf-8").read()
marker = "    public boolean generateWebAssembly(WebAssemblyCompilationOptions options) {\n"
assert marker in src and "ChecksTransformer" not in src, "Compiler.generateWebAssembly changed upstream: review this patch"
src = src.replace(marker, marker + """        // ---- added by the lab build: Java run-time checks in the student's classes
        for (var file : outputFiles.values()) {
            if (file.getName().endsWith(".class") && !labChecked.contains(file)) {
                file.data = lab.teavm.ChecksTransformer.transform(file.data);
                labChecked.add(file);
            }
        }
""", 1)
field = "    private final Map<String, FileData> outputFiles = new LinkedHashMap<>();\n"
assert field in src
src = src.replace(field, field + "    private final java.util.List<FileData> labChecked = new java.util.ArrayList<>();\n", 1)
open(path, "w", encoding="utf-8").write(src)
PY

cat >> compiler/build.gradle <<GRADLE

// ---- added by the VIT Civil Engineering Innovation Lab build (tools/browser-javac/build.sh)
def labPatches = file("$PATCHES")
tasks.named("prepareTeaVMClassLib") {
    inputs.file labPatches
    doLast {
        project.copy {
            from zipTree(labPatches)
            exclude "META-INF/**"
            into project.layout.buildDirectory.dir("tmp/classlib-runtime")
        }
    }
}
tasks.named("generateClassLib") {
    inputs.file "$SDK_ADDITIONS"
    args "$SDK_ADDITIONS"
}
GRADLE

# Fix in the compile-time API converter: it renames TeaVM's class files (TMap$Entry ->
# java/util/Map$Entry) but copied the InnerClasses, OuterClass and nest attributes unchanged, so
# javac could not see nested JDK types such as Map.Entry. Rename them the same way.
python3 - compiler/src/main/java/org/teavm/javac/StdlibConverter.java <<'PY'
import sys
path = sys.argv[1]
src = open(path, encoding="utf-8").read()
marker = "    class FieldVisitorImpl extends FieldVisitor {"
assert marker in src and "visitInnerClass" not in src, "StdlibConverter changed upstream: review this patch"
patch = """    // ---- added by the lab build (tools/browser-javac/build.sh)
    private boolean hidden(String renamed) {
        for (String excluded : EXCLUDED) {
            if (renamed.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        String renamed = rename(name);
        if ((access & Opcodes.ACC_PRIVATE) != 0 || hidden(renamed)) {
            return;
        }
        String outer = outerName == null ? null : rename(outerName);
        String simple = innerName;
        if (innerName != null && renamed.lastIndexOf('$') >= 0) {
            simple = renamed.substring(renamed.lastIndexOf('$') + 1);
        }
        super.visitInnerClass(renamed, outer, simple, access);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        super.visitOuterClass(rename(owner), name, descriptor == null ? null : renameMethodDesc(descriptor));
    }

    @Override
    public void visitNestHost(String nestHost) {
        super.visitNestHost(rename(nestHost));
    }

    @Override
    public void visitNestMember(String nestMember) {
        String renamed = rename(nestMember);
        if (!hidden(renamed)) {
            super.visitNestMember(renamed);
        }
    }

"""
src = src.replace(marker, patch + marker, 1)

# Second fix: a member whose signature mentions a type that is not part of the API (TeaVM internals
# such as org.teavm.runtime.fs.VirtualFileAccessor, or java.io.FileDescriptor, which TeaVM lacks)
# made javac reject every use of its class (FileWriter, FileReader, FileInputStream, ...). The
# converter now collects the API's class names first and leaves such members out.
old_main = "    public static void main(String[] args) throws IOException {\n        try (var output"
assert old_main in src, "StdlibConverter.main changed upstream: review this patch"
src = src.replace(old_main, """    static Set<String> known;

    private static boolean knownType(Type type) {
        while (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        return type.getSort() != Type.OBJECT || known == null || known.contains(type.getInternalName());
    }

    private static boolean knownDesc(String desc, boolean method) {
        if (!method) {
            return knownType(Type.getType(desc));
        }
        for (Type arg : Type.getArgumentTypes(desc)) {
            if (!knownType(arg)) {
                return false;
            }
        }
        return knownType(Type.getReturnType(desc));
    }

    private static void collectKnown(String[] args) throws IOException {
        var names = new HashSet<String>();
        for (var i = 1; i < args.length; ++i) {
            var file = new File(args[i]);
            if (file.isFile()) {
                try (var input = new ZipInputStream(new FileInputStream(args[i]))) {
                    ZipEntry entry;
                    while ((entry = input.getNextEntry()) != null) {
                        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                            collectName(input, names);
                        }
                    }
                }
            } else {
                try (var stream = Files.walk(file.toPath())) {
                    for (var path : (Iterable<java.nio.file.Path>) stream::iterator) {
                        if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".class")) {
                            try (var in = Files.newInputStream(path)) {
                                collectName(in, names);
                            }
                        }
                    }
                }
            }
        }
        known = names;
    }

    private static void collectName(InputStream input, Set<String> names) throws IOException {
        ClassReader reader = new ClassReader(input);
        StdlibConverter converter = new StdlibConverter(new ClassWriter(0), collectMethodRenames(reader));
        reader.accept(converter, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        if (converter.visible) {
            names.add(converter.className);
        }
    }

    public static void main(String[] args) throws IOException {
        collectKnown(args);
        try (var output""", 1)

old_m = "        desc = renameMethodDesc(desc);\n        if (!emittedMethods.add(name + desc)) {"
assert old_m in src, "StdlibConverter.visitMethod changed upstream: review this patch"
src = src.replace(old_m, """        desc = renameMethodDesc(desc);
        if (!knownDesc(desc, true)) {
            return null;
        }
        if (exceptions != null && known != null) {     // undeclared exception types: leave them out
            exceptions = java.util.Arrays.stream(exceptions).filter(e -> known.contains(rename(e)))
                    .toArray(String[]::new);
        }
        if (!emittedMethods.add(name + desc)) {""", 1)

old_f = "        desc = renameDesc(desc);\n        if (signature != null) {\n            signature = renameFieldSignature(signature);"
assert old_f in src, "StdlibConverter.visitField changed upstream: review this patch"
src = src.replace(old_f, """        desc = renameDesc(desc);
        if (!knownDesc(desc, false)) {
            return null;
        }
        if (signature != null) {
            signature = renameFieldSignature(signature);""", 1)

open(path, "w", encoding="utf-8").write(src)
PY

export JAVA_HOME="$JAVA25_HOME"
unset JAVA_TOOL_OPTIONS
./gradlew --no-daemon --quiet :compiler:buildWasmGC :compiler:buildTeaVMClassLib :compiler:generateClassLib

cp compiler/build/generated/teavm/wasm-gc/compiler.wasm* "$OUT"/
cp compiler/build/classlib/compile-classlib-teavm.bin compiler/build/classlib/runtime-classlib-teavm.bin "$OUT"/
rm -f "$OUT"/*.html
cat > "$OUT/NOTICE.txt" <<NOTICE
In-browser Java compiler used by the Lab page.

Built from teavm-javac commit $COMMIT (https://github.com/konsoletyper/teavm-javac),
Apache License 2.0, (c) Alexey Andreev. Contains the Java compiler from OpenJDK
(openjdk/jdk25u revision $JDK_REVISION), GNU General Public License version 2 with the
Classpath Exception (https://openjdk.org/legal/gplv2+ce.html), and the TeaVM class library
(Apache License 2.0), with additions from java/teavm-patches of this repository (MIT; the
Formatter and Math are modified copies of TeaVM's, Apache License 2.0).
Built $(date -u +%Y-%m-%dT%H:%MZ).
NOTICE
ls -la "$OUT"
