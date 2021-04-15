rootProject.name = "hivemq-community-edition"

pluginManagement {
    plugins {
        id("com.github.johnrengelman.shadow") version "${extra["plugin.shadow.version"]}"
        id("com.github.hierynomus.license") version "${extra["plugin.license.version"]}"
        id("org.owasp.dependencycheck") version "${extra["plugin.dependencycheck.version"]}"
        id("com.github.spotbugs") version "${extra["plugin.spotbugs.version"]}"
        id("de.thetaphi.forbiddenapis") version "${extra["plugin.forbiddenapis.version"]}"
        id("com.github.sgtsilvio.gradle.utf8") version "${extra["plugin.utf8.version"]}"
        id("com.github.sgtsilvio.gradle.metadata") version "${extra["plugin.metadata.version"]}"
        id("com.github.sgtsilvio.gradle.javadoc-links") version "${extra["plugin.javadoc-links.version"]}"
        id("io.github.gradle-nexus.publish-plugin") version "${extra["plugin.nexus-publish.version"]}"
        id("com.github.ben-manes.versions") version "${extra["plugin.versions.version"]}"
    }
}

if (file("../hivemq-extension-sdk").exists()) {
    includeBuild("../hivemq-extension-sdk")
} else {
    logger.warn(
        """
        ######################################################################################################
        You can not use the latest changes of or modify the hivemq-extension-sdk.
        Please checkout the hivemq-extension-sdk repository next to the hivemq-community-edition repository.
        Execute the following command from your project directory:
        git clone https://github.com/hivemq/hivemq-extension-sdk.git ../hivemq-extension-sdk
        You can also clone your fork:
        git clone https://github.com/<replace-with-your-fork>/hivemq-extension-sdk.git ../hivemq-extension-sdk
        ######################################################################################################
        """.trimIndent()
    )
}
