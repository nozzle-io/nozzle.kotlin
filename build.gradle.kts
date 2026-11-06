import groovy.json.JsonSlurper

plugins {
    kotlin("jvm") version "2.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.test {
    useJUnitPlatform()
    val enumManifest = layout.projectDirectory.file("deps/nozzle/tools/enum_manifest.json")
    inputs.file(enumManifest)
    doFirst {
        val manifest = JsonSlurper().parse(enumManifest.asFile) as Map<*, *>
        manifest.forEach { (family, members) ->
            (members as Map<*, *>).forEach { (cName, value) ->
                check(value is Int) { "$family.$cName must be an integer" }
                systemProperty("nozzle.enum.$family.$cName", value.toString())
            }
        }
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}
