import java.util.concurrent.ForkJoinPool
import java.util.concurrent.RecursiveTask

plugins {
    id("de.fayard.buildSrcVersions") version Versions.de_fayard_buildsrcversions_gradle_plugin
    application
    java
    kotlin("jvm") version Versions.org_jetbrains_kotlin_jvm_gradle_plugin
    id("com.github.johnrengelman.shadow") version
        Versions.com_github_johnrengelman_shadow_gradle_plugin
    checkstyle
    id("org.jlleitschuh.gradle.ktlint") version
        Versions.org_jlleitschuh_gradle_ktlint_gradle_plugin
}

val dingNetGroup = "KULeuven"

group = dingNetGroup
version = "1.2.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    // dependencies for DingNetSimulator
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.commons_logging)
    implementation(Libs.commons_cli)
    implementation(Libs.jfreechart)
    implementation(Libs.jxmapviewer2)
    implementation(Libs.forms_rt)
    implementation(Libs.commons_math3)
    implementation(Libs.gson)
    implementation(Libs.moquette_broker)
    implementation(Libs.konf)
    implementation(files(Util.downloadLibFromUrl(ExternalLib.mqtt_client_wrapper)))
    // dependencies for protelis application
    implementation(Libs.protelis)
    implementation(Libs.simplelatlng)
    implementation(Libs.commons_lang3)
    implementation(Libs.jpx)
    implementation(Libs.flexjson)
    // batch
    implementation(Libs.guava)
    // dependencies for test
    testImplementation(Libs.junit_jupiter)
    testImplementation(Libs.kotlintest_runner_junit5)
    testImplementation(Libs.mockk)
}

application {
    mainClassName = "Simulator"
}

tasks.shadowJar.configure {
    // removes "-all" from the jar name
    archiveClassifier.set("")
    exclude("**/*.kotlin_metadata")
    exclude("**/*.kotlin_module")
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = 1
}

tasks.withType<Checkstyle> {
    ignoreFailures = false
    maxWarnings = 0
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

val batch by tasks.register<DefaultTask>("batch") {
    val separator = System.getProperty("file.separator")
    val envFile: String = System.getProperty("user.home") +
        "$separator.DingNet${separator}config${separator}simulation${separator}acsos2020.xml"
    val outputDir: String by project
    val configDir = File(projectDir, ".temp")

    dependsOn("build")
    dependsOn(createConfigFile)
    doLast {
        val forkJoinPool = ForkJoinPool(Runtime.getRuntime().availableProcessors())
        val jobs = configDir.listFiles()
            .filter { it.extension == "toml" }
            .map {
                tasks.create<JavaExec>("run${it.nameWithoutExtension}") {
                    group = dingNetGroup
                    description = "Launches simulation ${it.nameWithoutExtension}"
                    main = "Simulator"
                    classpath = sourceSets["main"].runtimeClasspath
                    args(
                        "-cf", envFile,
                        "-nf", it,
                        "-of", outputDir
                    )
                }
            }
            .map {
                object : RecursiveTask<Int>() {
                    override fun compute(): Int {
                        it.exec()
                        return 1
                    }
                }
            }
        jobs.forEach { forkJoinPool.execute(it) }
        var finishedJobs = 0
        jobs.forEach {
            it.join()
            println("simulation finisched: ${++finishedJobs}/${jobs.size}")
        }
        configDir.deleteRecursively()
    }
}

val createConfigFile by tasks.register<JavaExec>("createConfigFile") {
    dependsOn("build")

    val configFile: String by project
    val configDir = File(projectDir, ".temp")

    if (!configDir.exists() || !configDir.isDirectory) {
        configDir.mkdir()
    }
    main = "it.unibo.gradle.CartesianProduct"
    args(configDir.absolutePath, configFile)
    classpath = sourceSets["main"].runtimeClasspath
}
