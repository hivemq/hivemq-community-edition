package com.hivemq.licensethirdparty

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.util.*

/**
 * Reads the `dependency-license.xml` file created by the `downloadLicenses` task and creates `licenses` and
 * `licenses.html` files in the configured [outputDirectory].
 */
abstract class UpdateThirdPartyLicensesTask : DefaultTask() {

    companion object {

        // defines the artifacts that should be ignored in the third-party license report
        private fun shouldIgnore(coordinates: Coordinates) =
            coordinates.group.startsWith("com.hivemq") && (coordinates.name != "hivemq-mqtt-client")

        // defines the license to choose, if multiple licenses are available for an artifact
        private val LICENSE_ORDER = listOf(
            KnownLicense.APACHE_2_0,
            KnownLicense.MIT,
            KnownLicense.MIT_0,
            KnownLicense.BOUNCY_CASTLE,
            KnownLicense.BSD_3_CLAUSE,
            KnownLicense.BSD_2_CLAUSE,
            KnownLicense.GO,
            KnownLicense.CC0_1_0,
            KnownLicense.PUBLIC_DOMAIN,
            KnownLicense.W3C_19980720,
            KnownLicense.EDL_1_0,
            KnownLicense.EPL_2_0,
            KnownLicense.EPL_1_0,
            KnownLicense.CDDL_1_1,
            KnownLicense.CDDL_1_0,
        )
    }

    @get:Input
    val projectName = project.objects.property<String>()

    @get:InputFile
    val dependencyLicense: RegularFileProperty = project.objects.fileProperty()

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    protected fun run() {
        val productName = projectName.get()
        val dependencyLicenseFile = dependencyLicense.get().asFile.absoluteFile
        val resultPlaintextFile = outputDirectory.get().asFile.resolve("licenses")
        val resultHtmlFile = outputDirectory.get().asFile.resolve("licenses.html")

        check(productName.isNotBlank()) { "Project name is blank" }
        if (resultPlaintextFile.exists()) {
            check(resultPlaintextFile.delete()) { "Could not delete file '$resultPlaintextFile'" }
        }
        if (resultHtmlFile.exists()) {
            check(resultHtmlFile.delete()) { "Could not delete file '$resultHtmlFile'" }
        }

        val xmlMapper = XmlMapper()
        xmlMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        val dependencies = xmlMapper.readValue(dependencyLicenseFile, DependencyReport.Root())
        val entries = TreeMap<String, Pair<Coordinates, KnownLicense>>()
        for (dependency in dependencies) {
            val nameParts = dependency.name.split(":")
            check(nameParts.size == 3) { "Invalid dependency '${dependency.name}'" }
            val coordinates = Coordinates(nameParts[0], nameParts[1], nameParts[2])
            if (shouldIgnore(coordinates)) continue
            val licenses = dependency.licenses.map { convertLicense(it, coordinates) }
            val chosenLicense = checkNotNull(chooseLicense(licenses)) { "License can not be determined for '$coordinates'" }
            entries[coordinates.moduleId] = Pair(coordinates, chosenLicense)
        }

        val licensePlaintext = StringBuilder()
        val licenseHtml = StringBuilder()
        licensePlaintext.addHeaderPlaintext(productName)
        licenseHtml.addHeaderHtml(productName)
        for ((coordinates, chosenLicense) in entries.values) {
            licensePlaintext.addLinePlaintext(coordinates, chosenLicense)
            licenseHtml.addLineHtml(coordinates, chosenLicense)
        }
        licensePlaintext.addFooterPlaintext()
        licenseHtml.addFooterHtml()

        resultPlaintextFile.writeText(licensePlaintext.toString())
        resultHtmlFile.writeText(licenseHtml.toString())
    }

    private fun convertLicense(license: DependencyReport.License, coordinates: Coordinates): License {
        val name = license.name
        val url = license.url
        return when {
            name.matches(".*Apache.*[\\s\\-v](2\\.0.*|2(\\s.*|$))".toRegex()) -> KnownLicense.APACHE_2_0
            name == "Bouncy Castle Licence" -> KnownLicense.BOUNCY_CASTLE
            name.matches("(.*BSD.*2.*[Cc]lause.*)|(.*2.*[Cc]lause.*BSD.*)".toRegex()) -> KnownLicense.BSD_2_CLAUSE
            name.matches("(.*BSD.*3.*[Cc]lause.*)|(.*3.*[Cc]lause.*BSD.*)|(.*[Nn]ew.*BSD.*)|(.*BSD.*[Nn]ew.*)".toRegex()) || (url == "https://opensource.org/licenses/BSD-3-Clause") -> KnownLicense.BSD_3_CLAUSE
            name == "CC0" -> KnownLicense.CC0_1_0
            url == "https://glassfish.dev.java.net/public/CDDLv1.0.html" -> KnownLicense.CDDL_1_0
            (url == "https://oss.oracle.com/licenses/CDDL+GPL-1.1") || (url == "https://github.com/javaee/javax.annotation/blob/master/LICENSE") || (url == "https://glassfish.java.net/public/CDDL+GPL_1_1.html") -> KnownLicense.CDDL_1_1
            name.matches(".*(EDL|Eclipse.*Distribution.*License).*1\\.0.*".toRegex()) -> KnownLicense.EDL_1_0
            name.matches(".*(EPL|Eclipse.*Public.*License).*1\\.0.*".toRegex()) -> KnownLicense.EPL_1_0
            name.matches(".*(EPL|Eclipse.*Public.*License).*2\\.0.*".toRegex()) -> KnownLicense.EPL_2_0
            name == "Go License" -> KnownLicense.GO
            name.matches(".*MIT(\\s.*|$)".toRegex()) -> KnownLicense.MIT
            name.matches(".*MIT-0.*".toRegex()) -> KnownLicense.MIT_0
            name == "Public Domain" -> KnownLicense.PUBLIC_DOMAIN
            url == "http://www.w3.org/Consortium/Legal/copyright-software-19980720" -> KnownLicense.W3C_19980720
            // from here license name and url are not enough to determine the exact license, so we checked the specific dependency manually
            (name == "BSD") && (((coordinates.group == "dk.brics") && (coordinates.name == "automaton")) || ((coordinates.group == "org.picocontainer") && (coordinates.name == "picocontainer")) || ((coordinates.group == "org.ow2.asm") && (coordinates.name == "asm"))) -> KnownLicense.BSD_3_CLAUSE
            else -> UnknownLicense(name, url)
        }
    }

    private fun chooseLicense(licenses: List<License>): KnownLicense? {
        var chosenLicense: KnownLicense? = null
        var indexOfChosenLicense = Int.MAX_VALUE
        for (license in licenses) {
            if (license is KnownLicense) {
                val indexOfLicense = LICENSE_ORDER.indexOf(license)
                if ((indexOfLicense != -1) && (indexOfLicense < indexOfChosenLicense)) {
                    chosenLicense = license
                    indexOfChosenLicense = indexOfLicense
                }
            }
        }
        return chosenLicense
    }

    private fun StringBuilder.addHeaderPlaintext(productName: String) = append(
        """
        Third Party Licenses
        ==============================
        
        $productName uses the following third party libraries:
        
         Module                                                                     | Version                                   | License ID    | License URL
        --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        
        """.trimIndent()
    )

    private fun StringBuilder.addHeaderHtml(productName: String) = append(
        """
        <head>
            <title>Third Party Licences</title>
            <style>
                table, th, td {
                    border: 1px solid black;
                    border-collapse: collapse;
                    border-spacing: 0;
                }

                th, td {
                    padding: 5px;
                }
            </style>
        </head>
        
        <body>
        
        <h2>Third Party Licenses</h2>
        <p>$productName uses the following third party libraries</p>
        
        <table>
            <tbody>
            <tr>
                <th>Module</th>
                <th>Version</th>
                <th>License ID</th>
                <th>License URL</th>
            </tr>
        
        """.trimIndent()
    )

    private fun StringBuilder.addLinePlaintext(coordinates: Coordinates, license: KnownLicense) =
        append(" ${"%-74s".format(coordinates.moduleId)} | ${"%-41s".format(coordinates.version)} | ${"%-13s".format(license.id)} | ${license.url}\n")

    private fun StringBuilder.addLineHtml(coordinates: Coordinates, license: KnownLicense) = append(
        """
        |    <tr>
        |        <td>${coordinates.moduleId}</td>
        |        <td>${coordinates.version}</td>
        |        <td>${license.id}</td>
        |        <td>
        |            <a href="${license.url}">${license.url}</a>
        |        </td>
        |        <td></td>
        |    </tr>
        |
        """.trimMargin()
    )

    private fun StringBuilder.addFooterPlaintext() = append(
        """
        --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        
        The open source code of the libraries can be obtained by sending an email to legal@hivemq.com.
        
        """.trimIndent()
    )

    private fun StringBuilder.addFooterHtml() = append(
        """
            </tbody>
        </table>
        <p>The open source code of the libraries can be obtained by sending an email to <a href="mailto:legal@hivemq.com">legal@hivemq.com</a>.
        </p>
        </body>
        
        """.trimIndent()
    )
}

data class Coordinates(val group: String, val name: String, val version: String) {
    val moduleId get() = "$group:$name"

    override fun toString() = "$group:$name:$version"
}
