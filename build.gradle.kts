import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.3.0"
    id("com.gradle.plugin-publish") version "1.3.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

group = "com.tazzledazzle.gradle"
version = property("version") as String

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

val functionalTestSourceSet =
    sourceSets.create("functionalTest") {
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }

dependencies {
    testImplementation(kotlin("test"))
    add("functionalTestImplementation", kotlin("test"))
    add("functionalTestImplementation", gradleTestKit())
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

ktlint {
    android.set(false)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("functionalTest") {
    description = "Runs functional tests."
    group = "verification"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    mustRunAfter(tasks.test)
}

tasks.check {
    dependsOn("detekt", "ktlintCheck", "functionalTest")
}

gradlePlugin {
    website.set("https://github.com/tazzledazzle/gradle-python-plugin")
    vcsUrl.set("https://github.com/tazzledazzle/gradle-python-plugin.git")
    plugins {
        create("pythonPlugin") {
            id = "com.tazzledazzle.python"
            implementationClass = "com.tazzledazzle.python.PythonPlugin"
            displayName = "Gradle Python Plugin"
            description =
                "Run Python tools from Gradle with Conda or uv environment bootstrap, " +
                "shared BuildService state, and configurable exit-code policy."
            tags.set(listOf("python", "conda", "uv", "gradle", "build"))
        }
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
    }
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "17"
}
