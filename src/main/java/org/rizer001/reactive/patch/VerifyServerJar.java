package org.rizer001.reactive.patch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Build-time verification: proves the Reactive patches actually landed in the
 * final server jar instead of being silently shadowed by vanilla classes.
 *
 * Checks:
 *  1. Every class that Reactive overrides in source (Eula, Settings, Commands,
 *     Services, StoredUserList, CachedUserNameToIdResolver) must be present in
 *     the final jar AND byte-different from the original vanilla jar.
 *  2. ServerLevel.class must differ from vanilla (ASM patch applied).
 *  3. The ASM hook target (ReactiveGameRuleHooks) must be referenced by
 *     ServerLevel bytecode.
 *  4. The Reactive entry point must be present.
 *  5. No Mojang signature entries may survive in the final jar.
 *
 * Usage: VerifyServerJar <final.jar> <original-vanilla.jar>
 */
public class VerifyServerJar {

    private static final List<String> SOURCE_OVERRIDES = List.of(
            "net/minecraft/server/Eula.class",
            "net/minecraft/server/dedicated/Settings.class",
            "net/minecraft/server/Services.class",
            "net/minecraft/server/players/StoredUserList.class",
            "net/minecraft/server/players/CachedUserNameToIdResolver.class",
            "net/minecraft/commands/Commands.class"
    );

    private static final String SERVER_LEVEL = "net/minecraft/server/level/ServerLevel.class";
    private static final String HOOK_REF = "org/rizer001/reactive/gamerules/ReactiveGameRuleHooks";
    private static final String ENTRY_POINT = "org/rizer001/reactive/server/StartMessages.class";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: VerifyServerJar <final.jar> <original-vanilla.jar>");
            System.exit(2);
        }
        Path finalJar = Path.of(args[0]);
        Path vanillaJar = Path.of(args[1]);

        if (!Files.isRegularFile(finalJar)) {
            System.err.println("[VerifyServerJar] FAILED: final jar not found: " + finalJar);
            System.exit(3);
        }

        List<String> problems = new java.util.ArrayList<>();

        try (ZipFile vanilla = new ZipFile(vanillaJar.toFile());
             ZipFile fin = new ZipFile(finalJar.toFile())) {

            Map<String, byte[]> vanillaBytes = readBytes(vanilla);

            // 1) source overrides must physically replace the vanilla classes
            for (String name : SOURCE_OVERRIDES) {
                byte[] orig = vanillaBytes.get(name);
                byte[] mine = readSingle(fin, name);
                if (orig == null) {
                    problems.add(name + ": class does not exist in the vanilla jar (vanilla layout changed?)");
                } else if (mine == null) {
                    problems.add(name + ": missing from final jar — Reactive override was NOT merged");
                } else if (java.util.Arrays.equals(orig, mine)) {
                    problems.add(name + ": final jar still contains the VANILLA class — Reactive override shadowed");
                }
            }

            // 2) ServerLevel must be the ASM-patched version
            byte[] levelOrig = vanillaBytes.get(SERVER_LEVEL);
            byte[] levelMine = readSingle(fin, SERVER_LEVEL);
            if (levelOrig == null) {
                problems.add(SERVER_LEVEL + ": class does not exist in the vanilla jar");
            } else if (levelMine == null) {
                problems.add(SERVER_LEVEL + ": missing from final jar");
            } else if (java.util.Arrays.equals(levelOrig, levelMine)) {
                problems.add(SERVER_LEVEL + ": not ASM-patched (byte-identical to vanilla)");
            } else if (!new String(levelMine, java.nio.charset.StandardCharsets.ISO_8859_1).contains(HOOK_REF)) {
                problems.add(SERVER_LEVEL + ": patched but does not reference " + HOOK_REF);
            }

            // 4) entry point present
            if (readSingle(fin, ENTRY_POINT) == null) {
                problems.add(ENTRY_POINT + ": missing from final jar");
            }

            // 5) no signature entries
            java.util.Enumeration<? extends ZipEntry> entries = fin.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (PatchVanilla.isSignatureEntry(name)) {
                    problems.add("signature entry survived in final jar: " + name);
                }
            }
        }

        if (!problems.isEmpty()) {
            System.err.println("[VerifyServerJar] FAILED — Reactive patches did not land in the server jar:");
            for (String p : problems) {
                System.err.println("  - " + p);
            }
            System.exit(1);
        }
        System.out.println("[VerifyServerJar] OK — " + (SOURCE_OVERRIDES.size() + 1)
                + " patched classes verified in " + finalJar.getFileName());
    }

    private static Map<String, byte[]> readBytes(ZipFile zip) {
        Map<String, byte[]> out = new java.util.HashMap<>();
        java.util.Enumeration<? extends ZipEntry> e = zip.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                out.put(entry.getName(), in.readAllBytes());
            } catch (IOException ex) {
                throw new RuntimeException("read " + entry.getName() + ": " + ex.getMessage(), ex);
            }
        }
        return out;
    }

    private static byte[] readSingle(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            return null;
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }
}
