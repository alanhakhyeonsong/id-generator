import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

tasks.named<BootJar>("bootJar").configure {
    enabled = true
    archiveFileName.set("app.jar")
}

tasks.named<Jar>("jar").configure {
    enabled = false
}
