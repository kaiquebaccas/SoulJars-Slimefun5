plugins {
    java
    id("com.gradleup.shadow") version "9.3.2"
    id("io.github.intisy.github-gradle") version "1.8.2.1"
}

group = "io.github.thebusybiscuit.souljars"
description = "SoulJars is a Slimefun addon that lets you capture the souls of slain mobs in jars."

// Shared Slimefun-addon build conventions (Java 8, spigot-api baseline, core dep, publish, shadow, version).
apply(from = "https://raw.githubusercontent.com/Slimefun5/gradle/stable/slimefun-addon.gradle")
