import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec

val frontendDir = file("../frontend")
val generatedWebUiDir = layout.buildDirectory.dir("generated/webui")

plugins {
    id("io.quarkus") version "3.33.4.1"
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/main/java"))
        resources.setSrcDirs(listOf("src/main/resources", generatedWebUiDir.get().asFile))
    }
    test {
        java.setSrcDirs(listOf("src/test/java"))
        resources.setSrcDirs(listOf("src/test/resources"))
    }
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.33.4.1"))
    implementation(project(":api"))
    implementation(project(":schema-core"))
    implementation("org.javastro.ivoa:ivoa-schema:0.9.14")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkiverse.quinoa:quarkus-quinoa:2.8.2")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

val buildFrontend = tasks.register<Exec>("buildFrontend") {
    workingDir = frontendDir
    commandLine("npm", "run", "build")
}

val prepareFrontendAssets = tasks.register<Copy>("prepareFrontendAssets") {
    dependsOn(buildFrontend)
    from(frontendDir.resolve("build"))
    into(generatedWebUiDir.map { it.dir("META-INF/resources") })
}

tasks.named("processResources") {
    dependsOn(prepareFrontendAssets)
}
