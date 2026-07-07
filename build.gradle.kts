plugins {
    java
    id("com.gradleup.shadow") version "9.3.2"
    id("io.github.intisy.github-gradle") version "1.8.2.1"
}

group = "io.github.thebusybiscuit.souljars"

fun latestGitTagVersion(): String? = try {
    val out = providers.exec { workingDir = rootDir; commandLine("git", "describe", "--tags", "--abbrev=0"); isIgnoreExitValue = true }
    if (out.result.get().exitValue == 0) out.standardOutput.asText.get().trim().removePrefix("gh-").removePrefix("v").takeIf { it.isNotBlank() } else null
} catch (e: Exception) { null }

version = (project.findProperty("artifact_version") as String?)?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: latestGitTagVersion() ?: "1.1.0"
val versionSuffix: String = when {
    !(project.findProperty("artifact_version") as String?).isNullOrBlank() -> ""
    System.getenv("GITHUB_ACTIONS") == "true" -> "-EXPERIMENTAL"
    else -> "-UNOFFICIAL"
}
val displayVersion = "${project.version}$versionSuffix"
description = "SoulJars is a Slimefun addon that lets you capture the souls of slain mobs in jars."

github {
    accessToken = System.getenv("GITHUB_TOKEN") ?: ""
    publish {
        tag = System.getenv("GITHUB_REF_NAME")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    // Spigot 1.16.5 is the Java-8-compatible API baseline the whole fork compiles against; version-specific
    // behaviour is routed through Slimefun's compat layer so the single jar still runs on 1.8 - 26.x.
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    githubCompileOnly("Slimefun5:Slimefun5:gh-v5.2.4.6")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to displayVersion)
        }
    }
    jar {
        enabled = false
    }
    shadowJar {
        archiveFileName.set("SoulJars-$displayVersion.jar")
        exclude("META-INF/**")
    }
    build {
        dependsOn(shadowJar)
    }
}
