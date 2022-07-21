++++
<p align="center">
  <img src="https://www.hivemq.com/img/svg/hivemq-ce.svg" width="500">
</p>
++++

= HiveMQ Community Edition

image:https://maven-badges.herokuapp.com/maven-central/com.hivemq/hivemq-community-edition-embedded/badge.svg["Maven Central",link="https://maven-badges.herokuapp.com/maven-central/com.hivemq/hivemq-community-edition-embedded"]
image:https://jitpack.io/v/hivemq/hivemq-community-edition.svg["JitPack", link="https://jitpack.io/#hivemq/hivemq-community-edition"]
image:https://javadoc.io/badge2/com.hivemq/hivemq-community-edition-embedded/javadoc.svg["javadoc", link=https://javadoc.io/doc/com.hivemq/hivemq-community-edition-embedded]
image:https://img.shields.io/github/workflow/status/hivemq/hivemq-community-edition/CI%20Check/master[GitHub Workflow Status (branch), link=https://github.com/hivemq/hivemq-community-edition/actions/workflows/check.yml?query=branch%3Amaster]

HiveMQ CE is a Java-based open source MQTT broker that fully supports MQTT 3.x and MQTT 5.

HiveMQ CE is the foundation of the HiveMQ enterprise-connectivity and messaging platform and implements all MQTT features.
This project is the technical core of many large MQTT deployments and is now available as open source software under the Apache 2 license.

* Website: https://www.hivemq.com/
* Documentation: https://github.com/hivemq/hivemq-community-edition/wiki
* Community Forum: https://community.hivemq.com/
* Contribution guidelines: link:CONTRIBUTING.adoc[Contributing.adoc]
* License: The source files in this repository are made available under the link:LICENSE[Apache License Version 2.0].

== Features

* All MQTT 3.1, 3.1.1 and MQTT 5.0 features
* MQTT over TCP, TLS, WebSocket and Secure WebSocket transport
* Java Extension SDK for:
** Authentication
** Authorization
** Client Initializers
** MQTT Packet Interceptors
** Interacting with Publishes, Retained Messages, Clients and Subscriptions
* Running on Windows, Linux and MacOS (Linux is recommended)
* Embedded Mode

HiveMQ CE is compatible with all MQTT 3 and MQTT 5 clients, including Eclipse Paho and https://github.com/hivemq/hivemq-mqtt-client[HiveMQ MQTT Client].

== Documentation

The documentation for the HiveMQ CE can be found https://github.com/hivemq/hivemq-community-edition/wiki[here].

MQTT Resources

* https://www.hivemq.com/mqtt-essentials/[MQTT Essentials]
* https://www.hivemq.com/mqtt-5/[MQTT 5 Essentials]

== HiveMQ Community Forum

The ideal place for questions or discussions about the HiveMQ Community Edition is our brand new https://community.hivemq.com/[HiveMQ Community Forum].

== How to Use

=== Quick Start

* Download the latest https://github.com/hivemq/hivemq-community-edition/releases/download/2022.1/hivemq-ce-2022.1.zip[HiveMQ CE binary package].
* Unzip the package.
* Run the run.sh (Linux/OSX) or run.bat (Windows) in the bin folder of the package.

[source,bash]
----
cd hivemq-ce-<version>
bin/run.sh
----

[IMPORTANT]
At least Java version 11 is required to run HiveMQ CE.
If you are in doubt, you can check the installed Java version by entering `java -version` on your command line.

You can now connect MQTT clients to `<ip address>:1883`.

[CAUTION]
If you want to connect devices on external networks to HiveMQ CE, please make sure your server is reachable from those networks and the required ports (default: 1883) are accessible through your firewall.

=== Just in Time Builds

Just in time builds for current branches on this repository and for specific commits are available https://hivemq.github.io/nightly-builds/[here].

=== Run with Docker

All releases as well as the current state of the `master` branch are available in the https://hub.docker.com/r/hivemq/hivemq-ce[hivemq/hivemq-ce] repository on DockerHub.
To execute this image, simply run the following command:

[source,bash]
----
docker run --name hivemq-ce -d -p 1883:1883 hivemq/hivemq-ce
----

To change the default log level you can set the environment variable `HIVEMQ_LOG_LEVEL` when running the container:

[source,bash]
----
docker run --name hivemq-ce -e HIVEMQ_LOG_LEVEL=INFO -d -p 1883:1883 hivemq/hivemq-ce
----

=== Building from Source

==== Building the Binary Package

Check out the git repository and build the binary package.

[source,bash]
----
git clone https://github.com/hivemq/hivemq-community-edition.git

cd hivemq-community-edition

./gradlew hivemqZip
----

The package `hivemq-ce-<version>.zip` is created in the sub-folder `build/distributions/`.

==== Building the Docker Image

Check out the git repository and build the Docker image.

[source,bash]
----
git clone https://github.com/hivemq/hivemq-community-edition.git

cd hivemq-community-edition

docker/build.sh
----

The Docker image `hivemq/hivemq-ce:snapshot` is created locally.

For further development instructions see the link:CONTRIBUTING.adoc[contribution guidelines].

=== Embedded Mode

HiveMQ Community Edition offers an embedded mode and a programmatic API for integrating with Java/Java EE software.

==== Gradle

If you use Gradle, include the following code in your `build.gradle(.kts)` file.

[source,groovy]
----

dependencies {
    implementation("com.hivemq:hivemq-community-edition-embedded:2022.1")
}
----

==== Maven

If you use Maven, include the following code in your `pom.xml` file.

[source,xml]
----
<project>
    ...
    <dependencies>
        <dependency>
            <groupId>com.hivemq</groupId>
            <artifactId>hivemq-community-edition-embedded</artifactId>
            <version>2022.1</version>
        </dependency>
    </dependencies>
    ...
</project>
----

NOTE: You must set the compiler version to `11` or higher.

==== Usage

Entry into the embedded mode is done with the `com.hivemq.embedded.EmbeddedHiveMQBuilder`.

[source,java]
----
public class Main {

    public static void main(String[] args) {

        final EmbeddedHiveMQBuilder embeddedHiveMQBuilder = EmbeddedHiveMQ.builder()
            .withConfigurationFolder(Path.of("/path/to/embedded-config-folder"))
            .withDataFolder(Path.of("/path/to/embedded-data-folder"))
            .withExtensionsFolder(Path.of("/path/to/embedded-extensions-folder"));
        ...
    }
}
----

Once built, an EmbeddedHiveMQ can be started with `start()`.

[source,java]
----
public class Main {

    public static void main(String[] args) {
        final EmbeddedHiveMQBuilder embeddedHiveMQBuilder = EmbeddedHiveMQ.builder();
        ...

        try (final EmbeddedHiveMQ hiveMQ = embeddedHiveMQBuilder.build()) {
            hiveMQ.start().join();
            ...
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
}
----

A running EmbeddedHiveMQ can be stopped with `stop()`.

[source,java]
----
public class Main {

    public static void main(String[] args) {

        ...

        try (final EmbeddedHiveMQ hiveMQ = embeddedHiveMQBuilder.build()) {
            ...
            hiveMQ.stop().join();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
}
----

Similar to the embedded HiveMQ an embedded extension can be built with the `com.hivemq.embedded.EmbeddedExtensionBuilder`.

Then add the embedded extension to the embedded HiveMQ builder.

[source,java]
----
public class Main {

    public static void main(String[] args) {

        final EmbeddedExtension embeddedExtension = EmbeddedExtension.builder()
                .withId("embedded-ext-1")
                .withName("Embedded Extension")
                .withVersion("1.0.0")
                .withPriority(0)
                .withStartPriority(1000)
                .withAuthor("Me")
                .withExtensionMain(new MyEmbeddedExtensionMain())
                .build();

        final EmbeddedHiveMQBuilder builder = EmbeddedHiveMQ.builder()
                .withConfigurationFolder(Path.of("/path/to/embedded-config-folder"))
                .withDataFolder(Path.of("/path/to/embedded-data-folder"))
                .withExtensionsFolder(Path.of("/path/to/embedded-extensions-folder"))
                .withEmbeddedExtension(embeddedExtension);

        try (final EmbeddedHiveMQ hiveMQ = builder.build()) {
            hiveMQ.start().join();
            //do something with hivemq
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class MyEmbeddedExtensionMain implements ExtensionMain {

        @Override
        public void extensionStart(final @NotNull ExtensionStartInput extensionStartInput, final @NotNull ExtensionStartOutput extensionStartOutput) {
            // my extension start code
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput extensionStopInput, final @NotNull ExtensionStopOutput extensionStopOutput) {
            // my extension stop code
        }
    }
}
----

NOTE: An EmbeddedHiveMQ is a resource that is similar to a e.g. network connection and implements the `java.lang.AutoCloseable` interface.
Always use ARM (_try with resources_) or ensure a call to `close()`.

==== Exclusions

When you deploy an application that includes EmbeddedHiveMQ, it can be useful to exclude some dependencies.
One way to exclude dependencies is with the  link:https://maven.apache.org/plugins/maven-shade-plugin/[maven shade plugin].

[source,xml]
----
<project>
...
 <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <artifactSet>
                                <excludes>
                                    <!--Exclude the undesired dependencies-->
                                    <exclude>org.rocksdb:rocksdbjni</exclude>
                                    <exclude>ch.qos.logback:logback-classic</exclude>
                                </excludes>
                            </artifactSet>
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
...
</project>
----

===== RocksDB Exclusion

To exclude the `org.rocksdb:rocksdbjni` dependency, two internal configurations must be changed before you call `start()`.

[source,java]
----
public class Main {

    public static void main(String[] args) {

        ...

        try (final EmbeddedHiveMQ hiveMQ = embeddedHiveMQBuilder.build()) {

            InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(PersistenceType.FILE);
            InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(PersistenceType.FILE);

            hiveMQ.start().join();

            ...
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }
}
----

== Contributing

If you want to contribute to HiveMQ CE, see the link:CONTRIBUTING.adoc[contribution guidelines].

== License

HiveMQ Community Edition is licensed under the `APACHE LICENSE, VERSION 2.0`.
A copy of the license can be found link:LICENSE[here].
