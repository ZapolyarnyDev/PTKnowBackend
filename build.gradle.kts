plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "io.github.zapolyarnydev"
version = "1.0.2"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)

	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.cache)
	implementation(libs.spring.boot.starter.json)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.flyway)

	implementation(libs.flyway.database.postgresql)

	runtimeOnly(libs.postgresql)

	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.oauth2.client)
	implementation(libs.spring.boot.starter.oauth2.resource.server)
	implementation(libs.springdoc.openapi.starter.webmvc.ui)
	implementation(libs.caffeine)
	implementation(libs.aws.sdk.s3)

	compileOnly(libs.mapstruct)
	annotationProcessor(libs.mapstruct.processor)

	implementation(libs.jnanoid)

	testImplementation(libs.spring.boot.starter.test)
	testRuntimeOnly(libs.junit.platform.launcher)
	testRuntimeOnly(libs.h2)
}

tasks.bootJar {
	archiveFileName.set("app.jar")
}

tasks.withType<JavaCompile> {
	options.release.set(25)
	options.encoding = "UTF-8"
}

tasks.withType<Test> {
	useJUnitPlatform()
}