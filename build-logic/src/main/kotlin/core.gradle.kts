plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

dependencies {
    "implementation"("org.jetbrains.kotlin:kotlin-stdlib")
    "implementation"("org.jetbrains.kotlin:kotlin-reflect")
    "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
    "implementation"("io.github.oshai:kotlin-logging-jvm:7.0.7")

    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    "testImplementation"("io.kotest:kotest-runner-junit5:5.8.0")
    "testImplementation"("io.kotest:kotest-assertions-core:5.8.0")
    "testImplementation"("io.kotest:kotest-property:5.8.0")
    "testImplementation"("io.mockk:mockk:1.14.6")
    "testImplementation"("com.ninja-squad:springmockk:5.0.1")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.12.2")
}
