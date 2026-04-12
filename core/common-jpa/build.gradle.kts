plugins {
    id("common")
    id("core")
    id("jpa")
}

dependencies {
    implementation(project(":system-core"))

    api("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.github.openfeign.querydsl:querydsl-jpa:${libs.versions.querydsl.get()}")
}

tasks.named<Jar>("jar").configure { enabled = true }
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar").configure { enabled = false }
