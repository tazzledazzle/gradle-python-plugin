plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.21"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

repositories {
    mavenCentral()
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
    plugins {
        create("pythonPlugin") {
            id = "com.example.python"
            implementationClass = "com.example.python.PythonPlugin"
        }
    }
}
