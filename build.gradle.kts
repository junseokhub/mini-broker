plugins {
	java
	application
}

group = "dev.minibroker"
version = "0.1.0-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	testImplementation(platform("org.junit:junit-bom:5.11.3"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.slf4j:slf4j-api:2.0.16")
	implementation("ch.qos.logback:logback-classic:1.5.12")

	implementation("org.openjdk.jmh:jmh-core:1.37")
	annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
	testImplementation("org.openjdk.jmh:jmh-core:1.37")
	testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

application {
	mainClass = "dev.minibroker.Main"
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	options.compilerArgs.add("-Xlint:all")
}