plugins {
    id("common")
    id("app")
    id("core")
    id("jpa")
}

dependencies {
    implementation(project(":system-core"))
    implementation(project(":common-jpa"))
    implementation(project(":common-valkey"))
    implementation(project(":common-lock"))
    implementation(project(":common-web"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // database
    runtimeOnly("org.postgresql:postgresql:${libs.versions.postgresql.get()}")
    implementation("org.flywaydb:flyway-core:${libs.versions.flyway.get()}")
    implementation("org.flywaydb:flyway-database-postgresql:${libs.versions.flyway.get()}")

    // querydsl
    implementation("io.github.openfeign.querydsl:querydsl-jpa:${libs.versions.querydsl.get()}")

    // documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${libs.versions.springdoc.openapi.get()}")

    // id generation
    implementation("com.fasterxml.uuid:java-uuid-generator:${libs.versions.java.uuid.generator.get()}")

    // logging (local)
    runtimeOnly("com.github.gavlyukovskiy:p6spy-spring-boot-starter:${libs.versions.p6spy.get()}")
}
