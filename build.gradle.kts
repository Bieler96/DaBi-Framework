plugins {
	kotlin("jvm") version "2.2.0"
}

group = "de.bieler.dabiframework"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation(kotlin("test"))
	testImplementation("org.mockito:mockito-core:5.12.0")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
	implementation("org.json:json:20231013")

	// exposed
	implementation("org.jetbrains.exposed:exposed-core:1.0.0-beta-5")
	implementation("org.jetbrains.exposed:exposed-jdbc:1.0.0-beta-5")
	implementation("com.h2database:h2:2.2.224")

	// mongodb
	implementation(platform("org.mongodb:mongodb-driver-bom:5.5.1"))
	implementation("org.mongodb:mongodb-driver-kotlin-coroutine")
	implementation("org.mongodb:bson-kotlinx")
}

tasks.test {
	useJUnitPlatform()
}
kotlin {
	jvmToolchain(21)
}