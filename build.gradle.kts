plugins {
    java
    application
}

group = "org.rizer001.reactive"
version = "26.2-1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// ---------------------------------------------------------------------------
// NMS inputs — everything is derived from the TRACKED vanilla jar.
// libs/server-26.2.jar         Mojang server 26.2 (official/Mojang-mapped names)
// libs/bundled/*.jar           Mojang's runtime libraries (guava, netty, gson, ...)
//
// Reactive works directly on the Mojang-mapped server (like Paper's
// moj-map runtime): we compile a handful of net.minecraft.* classes from source
// and merge them over the vanilla jar at build time. No remapping is needed.
// ---------------------------------------------------------------------------
val vanillaServerJar = file("libs/server-26.2.jar")
val bundledLibs = fileTree("libs/bundled") { include("*.jar") }

dependencies {
    // Vanilla server classes (compile against the Mojang jar itself)
    implementation(files(vanillaServerJar))
    // Vanilla runtime libraries
    implementation(bundledLibs)
    // Reactive config (TOML)
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    // ASM: build-time bytecode patching + verification (harmless on runtime classpath)
    implementation("org.ow2.asm:asm:9.9")
    implementation("org.ow2.asm:asm-tree:9.9")
}

application {
    mainClass.set("org.rizer001.reactive.server.StartMessages")
}

// The patched vanilla jar is a BUILD artifact — never committed, always rebuilt.
val patchedVanillaJar = layout.buildDirectory.file("server-26.2-patched.jar")

// ---------------------------------------------------------------------------
// patchVanilla — one deterministic ASM pass over the vanilla jar:
//   1. rewrites ServerLevel.getGameRules() to route through ReactiveGameRuleHooks
//   2. drops Mojang signature entries (content changed -> signatures invalid)
// Input : libs/server-26.2.jar (tracked)
// Output: build/server-26.2-patched.jar
// ---------------------------------------------------------------------------
tasks.register<JavaExec>("patchVanilla") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.rizer001.reactive.patch.PatchVanilla")
    args = listOf(
        vanillaServerJar.absolutePath,
        patchedVanillaJar.get().asFile.absolutePath
    )
    outputs.file(patchedVanillaJar)
}

// ---------------------------------------------------------------------------
// Final server jar = Reactive classes + ASM-patched vanilla + Mojang libs.
// duplicatesStrategy.EXCLUDE keeps the FIRST copy, so sourceSets output
// (our net/minecraft overrides) wins over the vanilla entry from the jar.
// ---------------------------------------------------------------------------
tasks.jar {
    dependsOn("patchVanilla")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "org.rizer001.reactive.server.StartMessages",
            "Implementation-Title" to "Reactive",
            "Implementation-Version" to version
        )
    }
    // 1) Reactive overrides first -> they replace vanilla entries on collision
    from(sourceSets.main.get().output)
    // 2) patched vanilla (all remaining net/minecraft classes + assets/data)
    from({ zipTree(patchedVanillaJar.get().asFile) })
    // 3) third-party runtime libs
    from({
        configurations.runtimeClasspath.get().filter {
            it.name.endsWith(".jar") && !it.name.startsWith("server-26.2")
        }.map { zipTree(it) }
    })
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SIG")
    exclude("META-INF/MOJANGCS.SF")
    exclude("META-INF/MOJANGCS.RSA")
}

// ---------------------------------------------------------------------------
// verifyServerJar — fail the build if any Reactive patch is missing from the
// final jar (this is the check that catches the old "EULA still enabled"
// class of bugs, where vanilla silently shadowed our classes at runtime).
// ---------------------------------------------------------------------------
// Default jar archive name: <project>-<version>.jar
val serverJarFile = layout.buildDirectory.file("libs/${project.name}-${version}.jar")

val verifyServerJar = tasks.register<JavaExec>("verifyServerJar") {
    dependsOn("jar")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.rizer001.reactive.patch.VerifyServerJar")
    args = listOf(serverJarFile.get().asFile.absolutePath, vanillaServerJar.absolutePath)
}
tasks.named("check") {
    dependsOn(verifyServerJar)
}

// ---------------------------------------------------------------------------
// run — deterministic: executes the SAME self-contained server jar that build
// produces. No classpath ordering games: a single jar, Reactive classes inside.
// ---------------------------------------------------------------------------
tasks.named<JavaExec>("run") {
    dependsOn("jar")
    // Lazy: resolved at execution time from the jar task's output
    classpath = files(tasks.jar.flatMap { it.archiveFile }.map { it.asFile })
    mainClass.set("org.rizer001.reactive.server.StartMessages")
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
    options.release = 25
}
