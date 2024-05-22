import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.nexusPublish)
    alias(libs.plugins.shadow)
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
    alias(libs.plugins.oci)
    alias(libs.plugins.javadocLinks)
    alias(libs.plugins.githubRelease)
    alias(libs.plugins.license)
    alias(libs.plugins.versions)

    /* Code Quality Plugins */
    jacoco
    alias(libs.plugins.forbiddenApis)

    id("com.hivemq.third-party-license-generator")
}

group = "com.hivemq"
description = "HiveMQ CE is a Java-based open source MQTT broker that fully supports MQTT 3.x and MQTT 5"

metadata {
    readableName = "HiveMQ Community Edition"
    organization {
        name = "HiveMQ GmbH"
        url = "https://www.hivemq.com/"
    }
    license {
        apache2()
    }
    developers {
        register("cschaebe") {
            fullName = "Christoph Schaebel"
            email = "christoph.schaebel@hivemq.com"
        }
        register("lbrandl") {
            fullName = "Lukas Brandl"
            email = "lukas.brandl@hivemq.com"
        }
        register("flimpoeck") {
            fullName = "Florian Limpoeck"
            email = "florian.limpoeck@hivemq.com"
        }
        register("sauroter") {
            fullName = "Georg Held"
            email = "georg.held@hivemq.com"
        }
        register("SgtSilvio") {
            fullName = "Silvio Giebl"
            email = "silvio.giebl@hivemq.com"
        }
    }
    github {
        issues()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.hivemq.extensionSdk)

    // netty
    implementation(libs.netty.buffer)
    implementation(libs.netty.codec)
    implementation(libs.netty.codec.http)
    implementation(libs.netty.common)
    implementation(libs.netty.handler)
    implementation(libs.netty.transport)

    // logging
    implementation(libs.slf4j.api)
    implementation(libs.julToSlf4j)
    implementation(libs.logback.classic)

    // security
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)

    // persistence
    implementation(libs.rocksdb)
    implementation(libs.xodus.openApi) {
        exclude("org.jetbrains", "annotations")
    }
    implementation(libs.xodus.environment) {
        exclude("org.jetbrains", "annotations")
    }
    // override transitive dependencies of xodus that have security vulnerabilities
    constraints {
        implementation(libs.kotlin.stdlib)
        implementation(libs.apache.commonsCompress)
    }

    // config
    implementation(libs.jaxb.api)
    runtimeOnly(libs.jaxb.impl)

    // metrics
    api(libs.dropwizard.metrics)
    implementation(libs.dropwizard.metrics.jmx)
    runtimeOnly(libs.dropwizard.metrics.logback)
    implementation(libs.oshi)
    // net.java.dev.jna:jna (transitive dependency of com.github.oshi:oshi-core) is used in imports

    // dependency injection
    implementation(libs.guice) {
        exclude("com.google.guava", "guava")
    }
    implementation(libs.javax.annotation.api)
    // javax.inject:javax.inject (transitive dependency of com.google.inject:guice) is used in imports

    // common
    implementation(libs.apache.commonsIO)
    implementation(libs.apache.commonsLang)
    implementation(libs.guava) {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
    // com.google.code.findbugs:jsr305 (transitive dependency of com.google.guava:guava) is used in imports
    implementation(libs.zeroAllocationHashing)
    implementation(libs.jackson.databind)
    implementation(libs.jctools)

    /* primitive data structures */
    implementation(libs.eclipse.collections)
}

/* ******************** test ******************** */

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockito)
    testImplementation(libs.equalsVerifier)
    testImplementation(libs.concurrentUnit)
    testImplementation(libs.shrinkwrap.api)
    testRuntimeOnly(libs.shrinkwrap.impl)
    testImplementation(libs.byteBuddy)
    testImplementation(libs.wiremock.jre8.standalone)
    testImplementation(libs.javassist)
    testImplementation(libs.awaitility)
    testImplementation(libs.stefanBirkner.systemRules) {
        exclude("junit", "junit-dep")
    }
}

tasks.test {
    minHeapSize = "128m"
    maxHeapSize = "2048m"
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.nio=ALL-UNNAMED",
        "--add-opens",
        "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens",
        "jdk.management/com.sun.management.internal=ALL-UNNAMED",
        "--add-exports",
        "java.base/jdk.internal.misc=ALL-UNNAMED",
    )

    val inclusions = rootDir.resolve("inclusions.txt")
    val exclusions = rootDir.resolve("exclusions.txt")
    if (inclusions.exists()) {
        include(inclusions.readLines())
    } else if (exclusions.exists()) {
        exclude(exclusions.readLines())
    }

    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED)
    }
}

/* ******************** distribution ******************** */

tasks.jar {
    manifest.attributes(
        "Implementation-Title" to "HiveMQ",
        "Implementation-Vendor" to metadata.organization.get().name.get(),
        "Implementation-Version" to project.version,
        "HiveMQ-Version" to project.version,
        "Main-Class" to "com.hivemq.HiveMQServer",
    )
}

tasks.shadowJar {
    mergeServiceFiles()
}

val hivemqZip by tasks.registering(Zip::class) {
    group = "distribution"

    val name = "hivemq-ce-${project.version}"

    archiveFileName = "$name.zip"

    from("src/distribution") { exclude("**/.gitkeep") }
    from("src/main/resources/config.xml") { into("conf") }
    from("src/main/resources/config.xsd") { into("conf") }
    from(tasks.shadowJar) { into("bin").rename { "hivemq.jar" } }
    into(name)
}

oci {
    registries {
        dockerHub {
            optionalCredentials()
        }
    }
    imageDefinitions.register("main") {
        imageName = "hivemq/hivemq-ce"
        allPlatforms {
            parentImages {
                add("library:eclipse-temurin:sha256!0f8bc645fb0c9ab40c913602c9f5f12c32d9ae6bef3e34fa0469c98e7341333c") // 21.0.3_9-jre-jammy
            }
            config {
                user = "10000"
                ports = setOf("1883", "8000")
                environment = mapOf(
                    "JAVA_OPTS" to "-XX:+UnlockExperimentalVMOptions -XX:+UseNUMA",
                    "HIVEMQ_ALLOW_ALL_CLIENTS" to "true",
                    "LANG" to "en_US.UTF-8",
                    "HOME" to "/opt/hivemq",
                )
                entryPoint = listOf("/opt/docker-entrypoint.sh")
                arguments = listOf("/opt/hivemq/bin/run.sh")
                volumes = setOf("/opt/hivemq/data", "/opt/hivemq/log")
                workingDirectory = "/opt/hivemq"
            }
            layers {
                layer("hivemq") {
                    contents {
                        into("opt") {
                            filePermissions = 0b110_100_000
                            directoryPermissions = 0b111_101_000
                            permissions("hivemq/", 0b111_111_000)
                            permissions("**/*.sh", 0b111_101_000)
                            from("docker/docker-entrypoint.sh")
                            into("hivemq") {
                                permissions("conf/", 0b111_111_000)
                                permissions("conf/config.xml", 0b110_110_000)
                                permissions("conf/logback.xml", 0b110_110_000)
                                permissions("data/", 0b111_111_000)
                                permissions("extensions/", 0b111_111_000)
                                permissions("extensions/*/", 0b111_111_000)
                                permissions("extensions/*/hivemq-extension.xml", 0b110_110_000)
                                permissions("log/", 0b111_111_000)
                                from("src/distribution") { filter { exclude("**/.gitkeep") } }
                                from("docker/config.xml") { into("conf") }
                                from("src/main/resources/config.xsd") { into("conf") }
                                from(tasks.shadowJar) { into("bin").rename(".*", "hivemq.jar") }
                            }
                        }
                    }
                }
            }
        }
        specificPlatform(platform("linux", "amd64"))
        specificPlatform(platform("linux", "arm64", "v8"))
    }
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("-html5")

    include("com/hivemq/embedded/*")

    doLast {
        javaexec {
            classpath("gradle/tools/javadoc-cleaner-1.0.jar")
        }
    }

    doLast { // javadoc search fix for jdk 11 https://bugs.openjdk.java.net/browse/JDK-8215291
        copy {
            from(destinationDir!!.resolve("search.js"))
            into(temporaryDir)
            filter { line -> line.replace("if (ui.item.p == item.l) {", "if (item.m && ui.item.p == item.l) {") }
        }
        delete(destinationDir!!.resolve("search.js"))
        copy {
            from(temporaryDir.resolve("search.js"))
            into(destinationDir!!)
        }
    }
}

/* ******************** checks ******************** */

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

forbiddenApis {
    bundledSignatures = setOf("jdk-system-out")
}

tasks.forbiddenApisMain {
    exclude("**/BatchedException.class")
    exclude("**/LoggingBootstrap.class")
}

tasks.forbiddenApisTest { enabled = false }

/* ******************** compliance ******************** */

license {
    header = file("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}

downloadLicenses {
    dependencyConfiguration = "runtimeClasspath"
}

tasks.updateThirdPartyLicenses {
    dependsOn(tasks.downloadLicenses)
    projectName = "HiveMQ"
    dependencyLicense = tasks.downloadLicenses.get().xmlDestination.resolve("dependency-license.xml")
    outputDirectory = layout.projectDirectory.dir("src/distribution/third-party-licenses")
}

/* ******************** publishing ******************** */

publishing {
    publications {
        register<MavenPublication>("distribution") {
            artifact(hivemqZip)
            artifactId = "hivemq-community-edition"
        }
        register<MavenPublication>("embedded") {
            from(components["java"])
            artifactId = "hivemq-community-edition-embedded"
        }
    }
}

signing {
    val signKey: String? by project
    val signKeyPass: String? by project
    useInMemoryPgpKeys(signKey, signKeyPass)
    sign(publishing.publications["embedded"])
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

githubRelease {
    token(System.getenv("GITHUB_TOKEN"))
    tagName(project.version.toString())
    releaseAssets(hivemqZip)
    allowUploadToExisting(true)
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations.shadowRuntimeElements.get()) {
    skip()
}
