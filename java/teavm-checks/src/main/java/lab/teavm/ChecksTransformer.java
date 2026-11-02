package lab.teavm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites the class files of students' programs so that they perform the run-time checks the
 * Java language requires but TeaVM's output omits (TeaVM also removes array reads and array
 * creations whose result is unused, so the checks must be in the bytecode before TeaVM reads
 * it). Every check calls lab.rt.Checks, which throws the JDK's exception with the JDK's message:
 * <ul>
 *   <li>array load and store: ArrayIndexOutOfBoundsException "Index 3 out of bounds for length 3"</li>
 *   <li>int and long division and remainder: ArithmeticException "/ by zero"; MIN_VALUE / -1 = MIN_VALUE</li>
 *   <li>array creation: NegativeArraySizeException "-1"</li>
 * </ul>
 * The stack effect of every rewritten instruction is unchanged, so stack map frames stay valid.
 * Used by web-bridge (course programs) and copied into the in-browser compiler.
 */
public final class ChecksTransformer {
    private static final String CHECKS = "lab/rt/Checks";

    private ChecksTransformer() {
    }

    /** Transformed class file. */
    public static byte[] transform(byte[] classFile) {
        ClassReader reader = new ClassReader(classFile);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                return mv == null ? null : new CheckingMethodVisitor(mv);
            }
        }, 0);
        return writer.toByteArray();
    }

    static final class CheckingMethodVisitor extends MethodVisitor {
        CheckingMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        private void call(String name, String descriptor) {
            super.visitMethodInsn(Opcodes.INVOKESTATIC, CHECKS, name, descriptor, false);
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case Opcodes.IALOAD:
                case Opcodes.LALOAD:
                case Opcodes.FALOAD:
                case Opcodes.DALOAD:
                case Opcodes.AALOAD:
                case Opcodes.BALOAD:
                case Opcodes.CALOAD:
                case Opcodes.SALOAD:
                    super.visitInsn(Opcodes.DUP2);
                    call("index", "(Ljava/lang/Object;I)V");
                    super.visitInsn(opcode);
                    return;
                case Opcodes.IASTORE:
                    call("store", "([III)V");
                    return;
                case Opcodes.LASTORE:
                    call("store", "([JIJ)V");
                    return;
                case Opcodes.FASTORE:
                    call("store", "([FIF)V");
                    return;
                case Opcodes.DASTORE:
                    call("store", "([DID)V");
                    return;
                case Opcodes.AASTORE:
                    call("store", "([Ljava/lang/Object;ILjava/lang/Object;)V");
                    return;
                case Opcodes.BASTORE:
                    call("storeByteOrBoolean", "(Ljava/lang/Object;II)V");
                    return;
                case Opcodes.CASTORE:
                    call("store", "([CII)V");
                    return;
                case Opcodes.SASTORE:
                    call("store", "([SII)V");
                    return;
                case Opcodes.IDIV:
                    call("div", "(II)I");
                    return;
                case Opcodes.IREM:
                    call("rem", "(II)I");
                    return;
                case Opcodes.LDIV:
                    call("div", "(JJ)J");
                    return;
                case Opcodes.LREM:
                    call("rem", "(JJ)J");
                    return;
                default:
                    super.visitInsn(opcode);
            }
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (opcode == Opcodes.NEWARRAY) {
                super.visitInsn(Opcodes.DUP);
                call("size", "(I)V");
            }
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.ANEWARRAY) {
                super.visitInsn(Opcodes.DUP);
                call("size", "(I)V");
            }
            super.visitTypeInsn(opcode, type);
        }
    }

    /** Transforms every .class file below the given directories in place. */
    public static void main(String[] args) throws IOException {
        int count = 0;
        for (String arg : args) {
            Path root = Path.of(arg);
            if (!Files.isDirectory(root)) {
                continue;
            }
            List<Path> files;
            try (Stream<Path> walk = Files.walk(root)) {
                files = walk.filter(p -> p.toString().endsWith(".class")).toList();
            }
            for (Path file : files) {
                Files.write(file, transform(Files.readAllBytes(file)));
                count++;
            }
        }
        System.out.println("run-time checks added to " + count + " class files");
    }
}
