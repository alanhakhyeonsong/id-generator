plugins {
    id("common")
    id("core")
}

dependencies {
    implementation(project(":common-valkey"))

    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework:spring-tx")
}

tasks.named<Jar>("jar").configure { enabled = true }
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar").configure { enabled = false }
