group = "it.pagopa.wallet"

version = "0.0.1-SNAPSHOT"

description = "pagopa-payment-wallet-cdc-service"

plugins {
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  id("org.springframework.boot") version "3.3.4"
  id("io.spring.dependency-management") version "1.1.6"
  id("com.diffplug.spotless") version "6.18.0"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  jacoco
  application
}

repositories {
  mavenCentral()
  mavenLocal()
}

object Dependencies {
  const val ecsLoggingVersion = "1.5.0"
  const val openTelemetryVersion = "1.37.0"
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

dependencyLocking { lockAllConfigurations() }

dependencyManagement {
  imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.0.5") }
  imports { mavenBom("com.azure.spring:spring-cloud-azure-dependencies:5.13.0") }
  // Kotlin BOM
  imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:1.7.22") }
  imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  // azure storage queue
  implementation("com.azure.spring:spring-cloud-azure-starter")
  implementation("com.azure.spring:spring-cloud-azure-starter-data-cosmos")
  implementation("com.azure:azure-storage-queue")
  implementation("com.azure:azure-core-serializer-json-jackson")

  // otel api
  implementation("io.opentelemetry:opentelemetry-api:${Dependencies.openTelemetryVersion}")

  implementation("co.elastic.logging:logback-ecs-encoder:${Dependencies.ecsLoggingVersion}")
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("io.projectreactor:reactor-test")
  // Kotlin dependencies
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  // Byte Buddy
  implementation("net.bytebuddy:byte-buddy:1.15.3")
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

springBoot {
  mainClass.set("it.pagopa.wallet.PagopaPaymentWalletCdcServiceApplicationKt")
  buildInfo {
    properties {
      additional.set(mapOf("description" to (project.description ?: "Default description")))
    }
  }
}

tasks.named<Jar>("jar") { enabled = false }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    toggleOffOn()
    targetExclude("build/**/*")
    ktfmt().kotlinlangStyle()
  }
  kotlinGradle {
    toggleOffOn()
    targetExclude("build/**/*.kts")
    ktfmt().googleStyle()
  }
  java {
    target("**/*.java")
    targetExclude("build/**/*")
    eclipse().configFile("eclipse-style.xml")
    toggleOffOn()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
  dependsOn(tasks.test) // tests are required to run before generating the report

  classDirectories.setFrom(
    files(
      classDirectories.files.map {
        fileTree(it).matching {
          exclude("it/pagopa/wallet/PagopaPaymentWalletCdcServiceApplicationKt.class")
        }
      }
    )
  )

  reports { xml.required.set(true) }
}

/**
 * Task used to expand application properties with build specific properties such as artifact name
 * and version
 */
tasks.processResources { filesMatching("application.properties") { expand(project.properties) } }
