import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    `maven-publish`
}

group = "com.github.Bieler96.DaBi-Framework"
version = "v1.0.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.classgraph:classgraph:4.8.165")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    implementation("org.json:json:20231013")

    // exposed
    implementation("org.jetbrains.exposed:exposed-core:1.0.0-beta-5")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.0.0-beta-5")
    implementation("org.jetbrains.exposed:exposed-java-time:1.0.0-beta-5")
    implementation("com.h2database:h2:2.2.224")

    // mongodb
    implementation(platform("org.mongodb:mongodb-driver-bom:5.5.1"))
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine")
    implementation("org.mongodb:mongodb-driver-kotlin-sync")
    implementation("org.mongodb:bson-kotlinx")

    // ktor-server
    implementation("io.ktor:ktor-server-status-pages:3.2.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.2")
    implementation("io.ktor:ktor-server-core:3.2.2")
    implementation("io.ktor:ktor-server-cors:3.2.2")
    implementation("io.ktor:ktor-server-netty:3.2.2")
    implementation("io.ktor:ktor-server-config-yaml:3.2.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.2")

    // ktor-client
    implementation("io.ktor:ktor-client-core:3.2.2")
    implementation("io.ktor:ktor-client-cio:3.2.2")
    implementation("io.ktor:ktor-client-websockets:3.2.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-java-parameters")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.register("release") {
    doLast {
        var newVersion = project.findProperty("newVersion") as String?
        if (newVersion == null) {
            print("Please enter the new version (e.g., v1.0.2): ")
            val reader = BufferedReader(InputStreamReader(System.`in`))
            newVersion = reader.readLine()
        }

        if (newVersion.isNullOrBlank()) {
            throw GradleException("Version cannot be empty.")
        }

        println("Releasing version: $newVersion")

        // Update build.gradle.kts
        val buildFile = project.buildFile
        val buildFileContent = buildFile.readText()
        val updatedBuildFileContent = buildFileContent.replaceFirst(Regex("version = \".*\""), "version = \"$newVersion\"")
        buildFile.writeText(updatedBuildFileContent)

        // Git add, commit, push
        exec {
            commandLine("/usr/bin/git", "add", buildFile.absolutePath)
        }
        exec {
            commandLine("/usr/bin/git", "commit", "-m", "Release $newVersion")
        }
        exec {
            commandLine("/usr/bin/git", "push")
        }

        // Git tag and push tag
        exec {
            commandLine("/usr/bin/git", "tag", newVersion)
        }
        exec {
            commandLine("/usr/bin/git", "push", "origin", newVersion)
        }

        // Create GitHub release
        println("Creating GitHub release...")
        exec {
            commandLine("gh", "release", "create", newVersion, "--title", "Release $newVersion", "--notes", "Released version $newVersion")
        }

        println("Successfully released version $newVersion")
    }
}
