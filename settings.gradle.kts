rootProject.name = "hivemq-community-edition"

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
