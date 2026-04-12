pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "id-generator"

include(
    // core modules
    ":common-jpa",
    ":common-valkey",
    ":common-lock",
    ":system-core",

    // service modules
    ":id-generation",
)

// core modules path mapping
project(":common-jpa").projectDir = file("core/common-jpa")
project(":common-valkey").projectDir = file("core/common-valkey")
project(":common-lock").projectDir = file("core/common-lock")
