plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

val agentVersion = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.14.18")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.shadowJar {
    archiveFileName.set("PatchLibAgent.jar")
    relocate("net.bytebuddy", "patch_lib.bytebuddy")
    manifest {
        attributes(
            "Premain-Class" to "patch_lib.agent.PreMain",
            "Can-Retransform-Classes" to "true",
            "Implementation-Version" to agentVersion,
        )
    }
}
