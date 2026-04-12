plugins {
    id("common")
    id("core")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.redisson:redisson-spring-boot-starter:${libs.versions.redisson.get()}")
    implementation("org.apache.commons:commons-pool2:${libs.versions.commons.pool2.get()}")
}

tasks.named<Jar>("jar").configure { enabled = true }
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar").configure { enabled = false }
