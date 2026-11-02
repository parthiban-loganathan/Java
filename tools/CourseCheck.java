import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Compiles every lesson and experiment in course/ with the JDK, runs it with each input file and
 * compares standard output with the matching expected file.
 *
 * <pre>
 *   java tools/CourseCheck.java                  # all programs that need no database
 *   java tools/CourseCheck.java --mysql          # also run programs marked requires=mysql
 *   java tools/CourseCheck.java --only E06a      # one program (id prefix of the folder name)
 * </pre>
 * Test cases: input.txt/expected.txt, input-2.txt/expected-2.txt, ... in the program folder.
 * Classpath entries for extra jars (for example a JDBC driver) can be given with --cp.
 */
public class CourseCheck {

    public static void main(String[] args) throws Exception {
        boolean mysql = false;
        String only = null;
        String extraCp = "";
        Path root = Paths.get("course");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--mysql": mysql = true; break;
                case "--only": only = args[++i]; break;
                case "--cp": extraCp = absoluteClasspath(args[++i]); break;
                case "--root": root = Paths.get(args[++i]); break;
                default: throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        List<Path> programs = new ArrayList<>();
        for (String group : new String[] {"lessons", "experiments"}) {
            try (Stream<Path> s = Files.list(root.resolve(group))) {
                programs.addAll(s.filter(p -> Files.exists(p.resolve("meta.properties"))).sorted().collect(Collectors.toList()));
            }
        }
        int pass = 0, fail = 0, skip = 0;
        for (Path dir : programs) {
            String id = dir.getFileName().toString();
            if (only != null && !id.startsWith(only)) continue;
            Properties meta = new Properties();
            try (InputStream in = Files.newInputStream(dir.resolve("meta.properties"))) {
                meta.load(in);
            }
            String requires = meta.getProperty("requires", "");
            if (requires.equals("mysql") && !mysql) {
                System.out.println("SKIP " + id + " (requires MySQL; run with --mysql)");
                skip++;
                continue;
            }
            String result = check(dir, meta.getProperty("main", "Main"), extraCp);
            if (result == null) {
                System.out.println("PASS " + id);
                pass++;
            } else {
                System.out.println("FAIL " + id + "\n" + result.indent(4));
                fail++;
            }
        }
        System.out.printf("%d passed, %d failed, %d skipped%n", pass, fail, skip);
        System.exit(fail == 0 ? 0 : 1);
    }

    /** Programs run in a temporary folder, so relative classpath entries are made absolute. */
    static String absoluteClasspath(String cp) {
        StringBuilder b = new StringBuilder();
        for (String part : cp.split(java.io.File.pathSeparator)) {
            if (part.isEmpty()) continue;
            if (b.length() > 0) b.append(java.io.File.pathSeparator);
            b.append(Paths.get(part).toAbsolutePath().normalize());
        }
        return b.toString();
    }

    static String check(Path dir, String mainClass, String extraCp) throws Exception {
        Path work = Files.createTempDirectory("coursecheck");
        Path classes = work.resolve("classes");
        Files.createDirectories(classes);
        List<String> sources = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) {
                sources.add(p.toAbsolutePath().toString());
            }
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<String> opts = new ArrayList<>(List.of("-d", classes.toString(), "-encoding", "UTF-8", "-Xlint:none"));
        if (!extraCp.isEmpty()) {
            opts.add("-cp");
            opts.add(extraCp);
        }
        opts.addAll(sources);
        java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
        int rc = javac.run(null, null, err, opts.toArray(new String[0]));
        if (rc != 0) {
            return "compilation failed:\n" + err.toString(StandardCharsets.UTF_8);
        }
        StringBuilder problems = new StringBuilder();
        for (int c = 1; ; c++) {
            String suffix = c == 1 ? "" : "-" + c;
            Path input = dir.resolve("input" + suffix + ".txt");
            Path expected = dir.resolve("expected" + suffix + ".txt");
            if (!Files.exists(expected)) {
                if (c == 1) return "missing expected.txt";
                break;
            }
            Path runDir = work.resolve("run" + c);
            Files.createDirectories(runDir);
            String cp = classes.toString() + (extraCp.isEmpty() ? "" : java.io.File.pathSeparator + extraCp);
            ProcessBuilder pb = new ProcessBuilder(
                    Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                    "-Dfile.encoding=UTF-8", "-Duser.language=en", "-Duser.country=US", "-cp", cp, mainClass)
                    .directory(runDir.toFile())
                    .redirectErrorStream(false);
            pb.environment().remove("JAVA_TOOL_OPTIONS");
            if (Files.exists(input)) pb.redirectInput(input.toFile());
            Process proc = pb.start();
            if (!Files.exists(input)) proc.getOutputStream().close();
            byte[] out = proc.getInputStream().readAllBytes();
            byte[] errOut = proc.getErrorStream().readAllBytes();
            if (!proc.waitFor(30, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                problems.append("case ").append(c).append(": timed out\n");
                continue;
            }
            String got = normalise(new String(out, StandardCharsets.UTF_8));
            String want = normalise(Files.readString(expected, StandardCharsets.UTF_8));
            if (!got.equals(want)) {
                problems.append("case ").append(c).append(": output differs\n--- expected\n").append(want)
                        .append("\n--- actual\n").append(got).append('\n');
                String e = new String(errOut, StandardCharsets.UTF_8);
                if (!e.isBlank()) problems.append("--- stderr\n").append(e).append('\n');
            }
        }
        return problems.length() == 0 ? null : problems.toString();
    }

    static String normalise(String s) {
        String[] lines = s.replace("\r\n", "\n").split("\n", -1);
        StringBuilder b = new StringBuilder();
        for (String l : lines) b.append(l.stripTrailing()).append('\n');
        return b.toString().strip();
    }
}
