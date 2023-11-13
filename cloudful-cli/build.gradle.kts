plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "de.jupiterpi.cloudful"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.0")
    implementation(platform("com.google.cloud:libraries-bom:26.26.0"))
    implementation("com.google.cloud:google-cloud-storage")
    implementation("commons-codec:commons-codec:1.15")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "de.jupiterpi.cloudful.cli.MainKt"
}

tasks.register<Copy>("installDev") {
    dependsOn("shadowJar")
    from(layout.buildDirectory.file("libs/cloudful-cli-1.0-SNAPSHOT-all.jar"))
    into(layout.projectDirectory.dir("dev_installation"))
}