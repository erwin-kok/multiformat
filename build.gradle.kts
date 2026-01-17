import com.adarshr.gradle.testlogger.theme.ThemeType
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "2.3.0"

    `java-library`
    `java-test-fixtures`
    signing
    `maven-publish`

    alias(libs.plugins.build.kover)
    alias(libs.plugins.build.ktlint)
    alias(libs.plugins.build.nexus)
    alias(libs.plugins.build.versions)
    alias(libs.plugins.build.testlogger)
    alias(libs.plugins.build.protobuf)
    alias(libs.plugins.build.serialization)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

group = "org.erwinkok.multiformat"
version = "1.2.0"

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.atomicfu)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlin.serialization)

    implementation(libs.ipaddress)
    implementation(libs.ktor.network)
    implementation(libs.result.monad)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)

    testImplementation(testFixtures(libs.result.monad))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.debug)

    testRuntimeOnly(libs.logback.classic)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

testlogger {
    theme = ThemeType.MOCHA
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.test {
    useJUnitPlatform()
}

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin", "build/generated/source/proto/main/java")
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "org.erwinkok.multiformat.multicodec.Codec",
                    "org.erwinkok.multiformat.multicodec.GenerateKt*",
                )
            }
        }

        verify {
            rule {
                bound {
                    minValue.set(0)
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set(project.name)
                description.set("Multiformats protocols")
                inceptionYear.set("2022")
                url.set("https://github.com/erwin-kok/multiformat")
                licenses {
                    license {
                        name.set("BSD-3-Clause")
                        url.set("https://opensource.org/licenses/BSD-3-Clause")
                    }
                }
                developers {
                    developer {
                        id.set("erwin-kok")
                        name.set("Erwin Kok")
                        email.set("erwin.kok@protonmail.com")
                        url.set("https://github.com/erwin-kok/")
                        roles.set(listOf("owner", "developer"))
                    }
                }
                scm {
                    url.set("https://github.com/erwin-kok/multiformat")
                    connection.set("scm:git:https://github.com/erwin-kok/multiformat")
                    developerConnection.set("scm:git:ssh://git@github.com:erwin-kok/multiformat.git")
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/erwin-kok/multiformat/issues")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
