package org.rizer001.reactive.patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Build-time tool: patches the vanilla ServerLevel.getGameRules() method so that
 * every world gets its OWN GameRules object instead of the server-wide one.
 *
 * Usage: PatchVanilla <input.jar> <output.jar>
 */
public class PatchVanilla {

    private static final String SERVER_LEVEL_CLASS = "net/minecraft/server/level/ServerLevel.class";
    private static final String HOOK_CLASS = "org/rizer001/reactive/gamerules/ReactiveGameRuleHooks";
    private static final String HOOK_DESC =
            "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/level/gamerules/GameRules;";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PatchVanilla <in.jar> <out.jar>");
            System.exit(2);
        }
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Path tmp = Path.of(args[1] + ".tmp");
        Files.createDirectories(out.getParent());

        int patched = 0;
        byte[] buffer = new byte[65536];
        try (JarInputStream jin = new JarInputStream(Files.newInputStream(in));
             JarOutputStream jout = new JarOutputStream(Files.newOutputStream(tmp))) {
            JarEntry entry;
            while ((entry = jin.getNextJarEntry()) != null) {
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                int n;
                while ((n = jin.read(buffer)) > 0) {
                    data.write(buffer, 0, n);
                }
                byte[] bytes = data.toByteArray();
                if (entry.getName().equals(SERVER_LEVEL_CLASS)) {
                    bytes = patchServerLevel(bytes);
                    patched++;
                }
                JarEntry outEntry = new JarEntry(entry.getName());
                jout.putNextEntry(outEntry);
                jout.write(bytes);
                jout.closeEntry();
            }
        }

        // Replace original with patched version
        Files.move(tmp, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        if (patched != 1) {
            System.err.println("[Reactive] FAILED: expected to patch exactly 1 ServerLevel.class, patched " + patched);
            System.exit(3);
        }
        System.out.println("[Reactive] Patched ServerLevel.getGameRules() -> per-world game rules");
    }

    private static byte[] patchServerLevel(byte[] original) throws IOException {
        ClassReader reader = new ClassReader(original);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        for (MethodNode method : node.methods) {
            if (!method.name.equals("getGameRules")) {
                continue;
            }
            if (!method.desc.equals("()Lnet/minecraft/world/level/gamerules/GameRules;")) {
                continue;
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call
                        && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && call.owner.equals("net/minecraft/server/MinecraftServer")
                        && call.name.equals("getGameRules")
                        && call.desc.equals("()Lnet/minecraft/world/level/gamerules/GameRules;")) {

                    InsnList replacement = new InsnList();
                    // stack: [..., server]  ->  push the level ('this'), then call the hook
                    replacement.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_CLASS, "getGameRules", HOOK_DESC, false));
                    method.instructions.insert(call, replacement);
                    method.instructions.remove(call);

                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    node.accept(writer);
                    return writer.toByteArray();
                }
            }
        }
        throw new IllegalStateException("[Reactive] getGameRules pattern not found in ServerLevel — vanilla layout changed?");
    }
}
