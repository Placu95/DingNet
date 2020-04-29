import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.RecursiveTask
import kotlin.math.min

plugins {
    id("de.fayard.buildSrcVersions") version Versions.de_fayard_buildsrcversions_gradle_plugin
    application
    java
    kotlin("jvm") version Versions.org_jetbrains_kotlin_jvm_gradle_plugin
    id("com.github.johnrengelman.shadow") version
        Versions.com_github_johnrengelman_shadow_gradle_plugin
    checkstyle
//    id("org.jlleitschuh.gradle.ktlint") version
//        Versions.org_jlleitschuh_gradle_ktlint_gradle_plugin
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
//    implementation(files(Util.downloadLibFromUrl(ExternalLib.mqtt_client_wrapper)))
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0")
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
        val numCores = runtime.availableProcessors()
        val files = configDir.listFiles().filter { it.extension == "toml" }.toMutableList()
        val classpath = "${project.buildDir.absolutePath}" +
            "${separator}libs${separator}$classpathJarName"
        while (files.isNotEmpty()) {
            val numOfJobs = min(files.size, numCores)
            val jobs = (0 until numOfJobs)
                .map { files.removeAt(0) }
                .map {
                    "java -Xmx1700m -cp $classpath Simulator -cf $envFile -nf $it -of $outputDir"
                }.map {
                    JobThread(runtime, it)
                }.map {
                    Pair(it, it.future)
                }
            jobs.forEach { it.first.start() }
            jobs.forEach { it.second.get() }
//            jobs.forEach { it.get() }
        }
    }
}

class JobThread(private val runtime: Runtime, private val cmd: String) : Thread() {
    val future: CompletableFuture<Int> = CompletableFuture()
    override fun run() {
        runtime.exec(cmd).onExit().get()
        future.complete(0)
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
        val numCores = runtime.availableProcessors()
        val forkJoinPool = ForkJoinPool(numCores)
        val files = configDir.listFiles().filter { it.extension == "toml" }.toMutableList()
        val classpath = "${project.buildDir.absolutePath}" +
            "${separator}libs${separator}$classpathJarName"
        while (files.isNotEmpty()) {
            val numOfJobs = min(files.size, numCores)
            val jobs = (0 until numOfJobs)
                .map { files.removeAt(0) }
                .map {
                    "java -Xmx1700m -cp $classpath Simulator -cf $envFile -nf $it -of $outputDir"
                }.map {
                    JobExec(runtime, it)
                }
            jobs.forEach { forkJoinPool.execute(it) }
            jobs.forEach { it.join() }
        }
    }
}

class JobExec(private val runtime: Runtime, private val cmd: String) : RecursiveTask<Int>() {
    override fun compute(): Int {
        runtime.exec(cmd).onExit().get()
        return 1
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
    if (configDir.exists() && configDir.isDirectory) {
        configDir.deleteRecursively()
    }
    configDir.mkdir()

    main = "it.unibo.gradle.CartesianProduct"
    args(configDir.absolutePath, configFile)
    classpath = sourceSets["main"].runtimeClasspath
}

class JobThread2(
    private val runtime: Runtime,
    private val listOfFiles: ListOfFiles,
    private val classpath: String,
    private val envFile: String,
    private val outputDir: String
) : Thread() {
    val future: CompletableFuture<Int> = CompletableFuture()
    private var stop = false
    override fun run() {
        while (!stop) {
            val file = listOfFiles.getNextFile()
            if (file == null) {
                stop = true
            } else {
                runtime.exec(getCmd(file)).onExit().get()
            }
        }
        future.complete(0)
    }
    private fun getCmd(file: String) =
        "java -Xmx1700m -cp $classpath Simulator -cf $envFile -nf $file -of $outputDir"
}

class ListOfFiles(list: List<String>) {
    private val files = list.toMutableList()
    fun getNextFile(): String? {
        synchronized(this) {
            return if (files.isNotEmpty()) {
                files.removeAt(0)
            } else {
                null
            }
        }
    }
}

val batchThread2 by tasks.register<DefaultTask>("batchThread2") {
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
        val files = ListOfFiles(configDir.listFiles()
            .filter { it.extension == "toml" }.map { it.absolutePath })
        val classpath = "${project.buildDir.absolutePath}" +
            "${separator}libs${separator}$classpathJarName"

        val jobs = (0 until numCores)
            .map {
                JobThread2(runtime, files, classpath, envFile, outputDir)
            }.map {
                Pair(it, it.future)
            }
        jobs.forEach { it.first.start() }
        jobs.forEach { it.second.get() }
//            jobs.forEach { it.get() }
    }
}

val searchMissingFiles by tasks.register<JavaExec>("searchMissingFiles") {
    dependsOn("build")
    main = "it.unibo.gradle.SearchMissingFiles"
    args(configDir.absolutePath, File(projectDir, "data").absolutePath)
    classpath = sourceSets["main"].runtimeClasspath
}

val refactorResults by tasks.register<JavaExec>("refactorResults") {
    dependsOn("build")
    main = "it.unibo.gradle.RefactorResults"
    args(File(projectDir, "data").absolutePath)
    classpath = sourceSets["main"].runtimeClasspath
}
