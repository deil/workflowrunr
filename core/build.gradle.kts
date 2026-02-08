plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.lombok")
    id("io.spring.dependency-management")
    id("io.freefair.lombok")
}

extra["kotlin.version"] = "2.2.20"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.7.18")
    }
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
