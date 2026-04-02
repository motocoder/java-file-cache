plugins {
    `java-library`
    `maven-publish`
}

group = "llc.berserkr"
version = "1.0.2"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(libs.slf4j.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.fest.assert)
    testImplementation(libs.slf4j.simple)
}

tasks.test {
    useJUnitPlatform()
    // Host-built native shared library lives in nativelib's host-native build dir
    val nativeLibDir = project(":nativelib").layout.buildDirectory.dir("host-native").get().asFile
    systemProperty("java.library.path", nativeLibDir.absolutePath)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "llc.berserkr"
            artifactId = "java-file-cache"
            version = "1.0.2"
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
