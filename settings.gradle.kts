pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.20"
        kotlin("plugin.lombok") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.spring") version "2.2.20"
    }
}

rootProject.name = "workflowrunr"
include("core", "example-spring")

findProject(":core")?.name = "workflowrunr-core"
