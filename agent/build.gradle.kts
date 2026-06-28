plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

val agentVersion = "0.2.0"

repositories {
    mavenCentral()
}

val coreDir = File(rootProject.extra["starsectorCoreDir"] as String)

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.14.18")

    //The shared, mod-facing API (annotations + PatchContext). Bundled into the agent jar (unrelocated)
    //so PatchContext resolves to a single class on the system loader at runtime.
    implementation(project(":api"))

    //Starsector core jars, same as the mod module. Provided at runtime by the game's classpath
    //on the system loader, so compile against them but do not bundle them into the agent jar.
    compileOnly(files(
        File(coreDir, "starfarer.api.jar"),
        File(coreDir, "starfarer_obf.jar"),
        File(coreDir, "commons-compiler.jar"),
        File(coreDir, "commons-compiler-jdk.jar"),
        File(coreDir, "fs.common_obf.jar"),
        File(coreDir, "fs.sound_obf.jar"),
        File(coreDir, "janino.jar"),
        File(coreDir, "jaxb-api-2.4.0-b180830.0359.jar"),
        File(coreDir, "jinput.jar"),
        File(coreDir, "jogg-0.0.7.jar"),
        File(coreDir, "jorbis-0.0.15.jar"),
        File(coreDir, "json.jar"),
        File(coreDir, "log4j-1.2.9.jar"),
        File(coreDir, "lwjgl.jar"),
        File(coreDir, "lwjgl_util.jar"),
        File(coreDir, "txw2-3.0.2.jar"),
        File(coreDir, "webp-imageio-0.1.6.jar"),
        File(coreDir, "xstream-1.4.10.jar"),
    ))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.shadowJar {
    archiveFileName.set("PatchLibAgent.jar")
    destinationDirectory.set(file("$rootDir/jars"))
    relocate("net.bytebuddy", "patchlib.bytebuddy")
    manifest {
        attributes(
            "Premain-Class" to "patchlib.agent.PreMain",
            "Can-Retransform-Classes" to "true",
            "Implementation-Version" to agentVersion,
        )
    }
}
