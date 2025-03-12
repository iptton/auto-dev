import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("java")
    alias(libs.plugins.serialization)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradleIntelliJPlugin)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.desktop)
    alias(libs.plugins.saliman.properties)
    id("idea")
}


repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/kpm/public")

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

dependencies {

    intellijPlatform {
        intellijIde(prop("ideaVersion"))
        jetbrainsRuntime()
        bundledPlugin("com.intellij.java")
//        bundledPlugin("org.jetbrains.kotlin")
    }

    implementation("io.reactivex.rxjava3:rxjava:3.1.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0") {
        excludeKotlinDeps()
    }
    implementation("com.squareup.okhttp3:okhttp:4.12.0") {
        excludeKotlinDeps()
    }
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0") {
        excludeKotlinDeps()
    }

    implementation("com.squareup.retrofit2:converter-jackson:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")

    implementation("org.commonmark:commonmark:0.21.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.21.0")

    implementation("org.yaml:snakeyaml:2.2")

    implementation("com.nfeld.jsonpathkt:jsonpathkt:2.0.1")

    implementation("org.jetbrains:markdown:0.6.1")

    // chocolate factorys
    // follow: https://onnxruntime.ai/docs/get-started/with-java.html
//        implementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")
//        implementation("ai.djl.huggingface:tokenizers:0.29.0")
    implementation("cc.unitmesh:cocoa-core:1.0.0") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        excludeKotlinDeps()
    }
//        implementation("cc.unitmesh:document:1.0.0")

    // kanban
    implementation("org.kohsuke:github-api:1.326")
    implementation("org.gitlab4j:gitlab4j-api:5.8.0")

    // template engine
    implementation("org.apache.velocity:velocity-engine-core:2.4.1")

    // token count
    implementation("com.knuddels:jtokkit:1.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
        exclude(group = "org.jetbrains.kotlinx")
    }

    val targetIdeVersion = providers.gradleProperty("targetIdeVersion").getOrElse("241")
    val jewelBridge = when (targetIdeVersion) {
        "241" -> rootProject.libs.jewel.bridge.ij241
        "243" -> rootProject.libs.jewel.bridge.ij243
        "251" -> rootProject.libs.jewel.bridge.ij251
        else -> rootProject.libs.jewel.bridge.ij241 // Default
    }
    implementation(jewelBridge)
}

task("resolveDependencies") {
    doLast {
        rootProject.allprojects
            .map { it.configurations }
            .flatMap { it.filter { c -> c.isCanBeResolved } }
            .forEach { it.resolve() }
    }
}

// TODO move to separate file
fun <T : ModuleDependency> T.excludeKotlinDeps() {
    exclude(module = "kotlin-runtime")
    exclude(module = "kotlin-reflect")
    exclude(module = "kotlin-stdlib")
    exclude(module = "kotlin-stdlib-common")
    exclude(module = "kotlin-stdlib-jdk8")
}


data class TypeWithVersion(val type: IntelliJPlatformType, val version: String)


fun String.toTypeWithVersion(): TypeWithVersion {
    val (code, version) = split("-", limit = 2)
    return TypeWithVersion(IntelliJPlatformType.fromCode(code), version)
}
fun IntelliJPlatformDependenciesExtension.intellijIde(versionWithCode: String) {
    val (type, version) = versionWithCode.toTypeWithVersion()
    create(type, version, useInstaller = false)
}
fun prop(name: String): String =
    extra.properties[name] as? String ?: error("Property `$name` is not defined in gradle.properties")


fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)