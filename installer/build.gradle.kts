plugins {
    java
    id("com.gradleup.shadow") version "9.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    //JNA is used to elevate to admin on Windows (ShellExecuteEx with the "runas" verb).
    //It is the only dependency, bundled into the installer jar by the shadow plugin.
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

//The installer is a standalone Swing app launched as its own process, so it bundles its dependencies
//and ships in the mods /jars folder, the same place the api jar lands.
tasks.shadowJar {
    archiveFileName.set("PatchLibInstaller.jar")
    destinationDirectory.set(file("$rootDir/jars"))
    manifest {
        attributes(
            "Main-Class" to "patchlib.installer.InstallerMain",
        )
    }
}
