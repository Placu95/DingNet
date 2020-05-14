import java.util.concurrent.CompletableFuture

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
version = "1.3.2"

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
    implementation(Libs.org_eclipse_paho_client_mqttv3)
    implementation(Libs.classgraph)
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

val configDir = File(projectDir, ".temp")

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

class Job(
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

val batch by tasks.register<DefaultTask>("batch") {
    val separator = System.getProperty("file.separator")
    val envFile: String = System.getProperty("user.home") +
        "$separator.DingNet${separator}config${separator}environment${separator}acsos2020.xml"
    val outputDir: String by project
    val outputDirFile = File(outputDir)
    if (!outputDirFile.exists() || !outputDirFile.isDirectory) {
        outputDirFile.mkdir()
    }

    dependsOn("build")
    dependsOn(createConfigFile)
    dependsOn(jar)
    doLast {
        val runtime = Runtime.getRuntime()
        val files = ListOfFiles(configDir.listFiles().filter { it.extension == "toml" }.map { it.absolutePath })
        val classpath = "${project.buildDir.absolutePath}${separator}libs${separator}$classpathJarName"
        val jobs = (0 until runtime.availableProcessors())
            .map { Job(runtime, files, classpath, envFile, outputDir) }
            .map { Pair(it, it.future) }
        jobs.forEach { it.first.start() }
        jobs.forEach { it.second.get() }
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
