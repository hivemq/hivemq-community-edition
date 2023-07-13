import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
    id("com.github.johnrengelman.shadow")
    id("io.github.sgtsilvio.gradle.defaults")
    id("io.github.sgtsilvio.gradle.metadata")
    id("io.github.sgtsilvio.gradle.javadoc-links")
    id("com.github.breadmoirai.github-release")
    id("com.github.hierynomus.license")
    id("org.owasp.dependencycheck")
    id("com.github.ben-manes.versions")

    /* Code Quality Plugins */
    id("jacoco")
    id("pmd")
    id("com.github.spotbugs")
    id("de.thetaphi.forbiddenapis")

    id("com.hivemq.third-party-license-generator")
}


/* ******************** metadata ******************** */

group = "com.hivemq"
description = "HiveMQ CE is a Java-based open source MQTT broker that fully supports MQTT 3.x and MQTT 5"

metadata {
    readableName.set("HiveMQ Community Edition")
    organization {
        name.set("HiveMQ GmbH")
        url.set("https://www.hivemq.com/")
    }
    license {
        apache2()
    }
    developers {
        register("cschaebe") {
            fullName.set("Christoph Schaebel")
            email.set("christoph.schaebel@hivemq.com")
        }
        register("lbrandl") {
            fullName.set("Lukas Brandl")
            email.set("lukas.brandl@hivemq.com")
        }
        register("flimpoeck") {
            fullName.set("Florian Limpoeck")
            email.set("florian.limpoeck@hivemq.com")
        }
        register("sauroter") {
            fullName.set("Georg Held")
            email.set("georg.held@hivemq.com")
        }
        register("SgtSilvio") {
            fullName.set("Silvio Giebl")
            email.set("silvio.giebl@hivemq.com")
        }
    }
    github {
        org.set("hivemq")
        repo.set("hivemq-community-edition")
        issues()
    }
}


/* ******************** java ******************** */

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    withJavadocJar()
    withSourcesJar()
}


/* ******************** dependencies ******************** */

repositories {
    mavenCentral()
}

dependencies {
    api("com.hivemq:hivemq-extension-sdk:${property("hivemq-extension-sdk.version")}")

    // netty
    implementation("io.netty:netty-buffer:${property("netty.version")}")
    implementation("io.netty:netty-codec:${property("netty.version")}")
    implementation("io.netty:netty-codec-http:${property("netty.version")}")
    implementation("io.netty:netty-common:${property("netty.version")}")
    implementation("io.netty:netty-handler:${property("netty.version")}")
    implementation("io.netty:netty-transport:${property("netty.version")}")

    // logging
    implementation("org.slf4j:slf4j-api:${property("slf4j.version")}")
    implementation("org.slf4j:jul-to-slf4j:${property("slf4j.version")}")
    implementation("ch.qos.logback:logback-classic:${property("logback.version")}")

    // security
    implementation("org.bouncycastle:bcprov-jdk18on:${property("bouncycastle.version")}")
    implementation("org.bouncycastle:bcpkix-jdk18on:${property("bouncycastle.version")}")

    // persistence
    implementation("org.rocksdb:rocksdbjni:${property("rocksdb.version")}")
    implementation("org.jetbrains.xodus:xodus-openAPI:${property("xodus.version")}") {
        exclude("org.jetbrains", "annotations")
    }
    implementation("org.jetbrains.xodus:xodus-environment:${property("xodus.version")}") {
        exclude("org.jetbrains", "annotations")
    }
    // override transitive dependencies of xodus that have security vulnerabilities
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:${property("kotlin.version")}")
        implementation("org.apache.commons:commons-compress:${property("commons-compress.version")}")
    }

    // config
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:${property("jakarta-xml-bind.version")}")
    runtimeOnly("com.sun.xml.bind:jaxb-impl:${property("jaxb.version")}")

    // metrics
    api("io.dropwizard.metrics:metrics-core:${property("metrics.version")}")
    implementation("io.dropwizard.metrics:metrics-jmx:${property("metrics.version")}")
    runtimeOnly("io.dropwizard.metrics:metrics-logback:${property("metrics.version")}")
    implementation("com.github.oshi:oshi-core:${property("oshi.version")}")
    // net.java.dev.jna:jna (transitive dependency of com.github.oshi:oshi-core) is used in imports

    // dependency injection
    implementation("com.google.inject:guice:${property("guice.version")}") {
        exclude("com.google.guava", "guava")
    }
    implementation("javax.annotation:javax.annotation-api:${property("javax.annotation.version")}")
    // javax.inject:javax.inject (transitive dependency of com.google.inject:guice) is used in imports

    // common
    implementation("commons-io:commons-io:${property("commons-io.version")}")
    implementation("org.apache.commons:commons-lang3:${property("commons-lang.version")}")
    implementation("com.google.guava:guava:${property("guava.version")}") {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
    // com.google.code.findbugs:jsr305 (transitive dependency of com.google.guava:guava) is used in imports
    implementation("net.openhft:zero-allocation-hashing:${property("zero-allocation-hashing.version")}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${property("jackson.version")}")
    implementation("org.jctools:jctools-core:${property("jctools.version")}")

    /* primitive data structures */
    implementation("org.eclipse.collections:eclipse-collections:${property("eclipse.collections.version")}")
}


/* ******************** test ******************** */

dependencies {
    testImplementation("junit:junit:${property("junit.version")}")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:${property("equalsverifier.version")}")
    testImplementation("net.jodah:concurrentunit:${property("concurrentunit.version")}")
    testImplementation("org.jboss.shrinkwrap:shrinkwrap-api:${property("shrinkwrap.version")}")
    testRuntimeOnly("org.jboss.shrinkwrap:shrinkwrap-impl-base:${property("shrinkwrap.version")}")
    testImplementation("net.bytebuddy:byte-buddy:${property("bytebuddy.version")}")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:${property("wiremock.version")}")
    testImplementation("org.javassist:javassist:${property("javassist.version")}")
    testImplementation("org.awaitility:awaitility:${property("awaitility.version")}")
    testImplementation("com.github.stefanbirkner:system-rules:${property("system-rules.version")}") {
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
        "java.base/jdk.internal.misc=ALL-UNNAMED"
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
        "Main-Class" to "com.hivemq.HiveMQServer"
    )
}

tasks.shadowJar {
    mergeServiceFiles()
}

val hivemqZip by tasks.registering(Zip::class) {
    group = "distribution"

    val name = "hivemq-ce-${project.version}"

    archiveFileName.set("$name.zip")

    from("src/distribution") { exclude("**/.gitkeep") }
    from("src/main/resources/config.xml") { into("conf") }
    from("src/main/resources/config.xsd") { into("conf") }
    from(tasks.shadowJar) { into("bin").rename { "hivemq.jar" } }
    into(name)
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
    toolVersion = "${property("jacoco.version")}"
}

pmd {
    toolVersion = "${property("pmd.version")}"
    sourceSets = listOf(project.sourceSets.main.get())
    isIgnoreFailures = true
    rulesMinimumPriority.set(3)
}

spotbugs {
    toolVersion.set("${property("spotbugs.version")}")
    ignoreFailures.set(true)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.8.0")
}

dependencyCheck {
    analyzers.apply {
        centralEnabled = false
    }
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
    scanConfigurations = listOf("runtimeClasspath")
    suppressionFile = "$projectDir/gradle/dependency-check/suppress.xml"
}

tasks.check { dependsOn(tasks.dependencyCheckAnalyze) }

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
    projectName.set("HiveMQ")
    dependencyLicense.set(tasks.downloadLicenses.get().xmlDestination.resolve("dependency-license.xml"))
    outputDirectory.set(layout.projectDirectory.dir("src/distribution/third-party-licenses"))
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
