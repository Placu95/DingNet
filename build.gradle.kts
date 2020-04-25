import java.util.concurrent.CompletableFuture
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

val numOfCore by tasks.register<DefaultTask>("numOfCore") {
    println(Runtime.getRuntime().availableProcessors())
}

val configDir = File(projectDir, ".temp")
val batchThread by tasks.register<DefaultTask>("batchThread") {
    val separator = System.getProperty("file.separator")
    val envFile: String = System.getProperty("user.home") +
        "$separator.DingNet${separator}config${separator}simulation${separator}acsos2020.xml"
    val outputDir: String by project

    dependsOn("build")
    dependsOn(createConfigFile)
    dependsOn(jar)
    doLast {
        val runtime = Runtime.getRuntime()
        val numCores = runtime.availableProcessors() / 2
        val files = configDir.listFiles().filter { it.extension == "toml" }.toMutableList()
        val classpath = "${project.buildDir.absolutePath}" +
            "${separator}libs${separator}$classpathJarName"
        while (files.isNotEmpty()) {
            val numOfJobs = if (files.size > numCores) numCores else files.size
            val jobs = (0 until numOfJobs)
                .map { files.removeAt(0) }
                .map {
                    "java -cp $classpath Simulator -cf $envFile -nf $it -of $outputDir"
                }.map {
                    object : Thread() {
                        val future: CompletableFuture<Int> = CompletableFuture()
                        override fun run() {
                            runtime.exec(it).onExit().get()
                            future.complete(0)
                        }
                    }
                }.map {
                    Pair(it, it.future)
                }
            jobs.forEach { it.first.start() }
            jobs.forEach { it.second.get() }
//            jobs.forEach { it.get() }
        }
    }
}

val batchExecutor by tasks.register<DefaultTask>("batchExecutor") {
    val separator = System.getProperty("file.separator")
    val envFile: String = System.getProperty("user.home") +
        "$separator.DingNet${separator}config${separator}simulation${separator}acsos2020.xml"
    val outputDir: String by project

    dependsOn("build")
    dependsOn(createConfigFile)
    dependsOn(jar)
    doLast {
        val runtime = Runtime.getRuntime()
        val numCores = runtime.availableProcessors() / 2
        val forkJoinPool = ForkJoinPool(numCores)
        val files = configDir.listFiles().filter { it.extension == "toml" }.toMutableList()
        val classpath = "${project.buildDir.absolutePath}" +
            "${separator}libs${separator}$classpathJarName"
        while (files.isNotEmpty()) {
            val numOfJobs = if (files.size > numCores) numCores else files.size
            val jobs = (0 until numOfJobs)
                .map { files.removeAt(0) }
                .map {
                    "java -cp $classpath Simulator -cf $envFile -nf $it -of $outputDir"
                }.map {
                    object : RecursiveTask<Int>() {
                        override fun compute(): Int {
                            runtime.exec(it).onExit().get()
                            return 1
                        }
                    }
                }
            jobs.forEach { forkJoinPool.execute(it) }
            jobs.forEach { it.join() }
        }
    }
}

val classpathJarName = "classpath.jar"
val jar by tasks.getting(Jar::class) {
    archiveName = classpathJarName
    manifest {
        attributes["Class-Path"] = files(configurations.runtimeClasspath)
            .map { it.toURI() }
            .joinToString(" ")
    }
}

val createConfigFile by tasks.register<JavaExec>("createConfigFile") {
    dependsOn("build")

    val configFile: String by project

    if (!configDir.exists() || !configDir.isDirectory) {
        configDir.mkdir()
    }
    main = "it.unibo.gradle.CartesianProduct"
    args(configDir.absolutePath, configFile)
    classpath = sourceSets["main"].runtimeClasspath
}
