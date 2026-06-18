plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.jar {
    archiveFileName.set("PatchLibAPI.jar")
    destinationDirectory.set(file("$rootDir/jars"))
}
