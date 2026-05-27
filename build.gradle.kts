plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

dependencies {
    testImplementation(kotlin("test"))
    add("functionalTestImplementation", kotlin("test"))
    add("functionalTestImplementation", gradleTestKit())
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

gradlePlugin {
    plugins {
        create("pythonPlugin") {
            id = "com.example.python"
            implementationClass = "com.example.python.PythonPlugin"
        }
    }
}
