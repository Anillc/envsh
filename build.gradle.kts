import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin ("jvm") version "1.6.10"
  id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "cn.anillc"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.2.4"

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
  implementation("com.zaxxer:nuprocess:2.0.2")
  implementation(kotlin("stdlib-jdk8"))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Class" to "cn.anillc.envsh.MainKt"))
  }
}