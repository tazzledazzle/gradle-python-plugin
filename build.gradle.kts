plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("pythonPlugin") {
            id = "com.example.python"
            implementationClass = "com.example.python.PythonPlugin"
        }
    }
}
