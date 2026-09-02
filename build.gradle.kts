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

dependencies {
    implementation(files("libs/server-26.2-stripped.jar"))
    implementation(fileTree("libs/bundled") { include("*.jar") })
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("org.ow2.asm:asm:9.9")
    implementation("org.ow2.asm:asm-tree:9.9")
}

application {
    mainClass.set("org.rizer001.reactive.server.StartMessages")
}

// Task: Patch ServerLevel.getGameRules() for per-world game rules
// Reads from stripped.jar, writes to patched.jar
tasks.register<JavaExec>("patchVanilla") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.rizer001.reactive.patch.PatchVanilla")
    args = listOf(
        "libs/server-26.2-stripped.jar",
        "libs/server-26.2-patched.jar"
    )
    dependsOn("classes")
}

// Configure run task to use patched jar
tasks.named<JavaExec>("run") {
    classpath = files("libs/server-26.2-patched.jar") +
                sourceSets.main.get().output +
                configurations.runtimeClasspath.get()
    mainClass.set("org.rizer001.reactive.server.StartMessages")
    dependsOn("classes", "patchVanilla")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn("patchVanilla")
    manifest {
        attributes(
            "Main-Class" to "org.rizer001.reactive.server.StartMessages",
            "Implementation-Title" to "Reactive",
            "Implementation-Version" to version
        )
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    // Include patched jar instead of stripped
    from(zipTree("libs/server-26.2-patched.jar"))
    // Include bundled deps (but exclude server jar from runtimeClasspath since we already included patched)
    from({
        configurations.runtimeClasspath.get().filter {
            it.name.endsWith(".jar") && !it.name.startsWith("server-26.2")
        }.map { zipTree(it) }
    })
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/MOJANGCS.SF")
    exclude("META-INF/MOJANGCS.RSA")
    exclude("META-INF/*.SIG")
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
    options.release = 25
}
