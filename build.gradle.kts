plugins {
    `java`
    id("io.github.intisy.github-gradle") version "1.8.2.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")

    "githubCompileOnly"("Slimefun5:Slimefun5:v5.2.3")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}