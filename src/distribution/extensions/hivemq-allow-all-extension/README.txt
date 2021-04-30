:hivemq-link: http://www.hivemq.com
:hivemq-extension-docs-link: http://www.hivemq.com/docs/extensions/latest/
:hivemq-extension-docs-archetype-link: http://www.hivemq.com/docs/extensions/latest/#maven-archetype-chapter
:hivemq-blog-tools: http://www.hivemq.com/mqtt-toolbox
:maven-documentation-profile-link: http://maven.apache.org/guides/introduction/introduction-to-profiles.html
:hivemq-support: http://www.hivemq.com/support/

== HiveMQ Allow All Extension

*Type*: Security

*Version*: 1.0.0

*License*: Apache License Version 2.0

=== Purpose

This Extension allows all MQTT clients to connect to HiveMQ.
Using this extension is not secure.
For production usage, add an appropriate security extension and remove the hivemq-allow-all extension.
You can download security extensions from the HiveMQ Marketplace (https://www.hivemq.com/extensions/).

=== Installation

. Clone this repository into a Java 11 maven project.
. Run `mvn package` goal from Maven to build the extension.
. Move the file: "target/hivemq-no-auth-extension-1.0.0-distribution.zip" to the directory: "HIVEMQ_HOME/extensions"
. Unzip the file.
. Start HiveMQ.

=== Need help?

If you encounter any problems, we are happy to help.
The best place to get in contact is our {hivemq-support}[support].
