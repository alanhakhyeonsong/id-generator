plugins {
    id("common")
    id("core")
}

dependencies {
    implementation(project(":system-core"))

    api("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.data:spring-data-commons:${libs.versions.spring.data.commons.get()}")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.cloud:spring-cloud-starter-openfeign")
    api("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${libs.versions.springdoc.openapi.get()}")
}

tasks.named<Jar>("jar").configure { enabled = true }
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar").configure { enabled = false }
