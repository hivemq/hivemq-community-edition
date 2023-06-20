package com.hivemq.licensethirdparty

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

class DependencyReport {

    class Root : TypeReference<List<Dependency>>()

    class Dependency(

        @param:JacksonXmlProperty(localName = "name", isAttribute = true)
        val name: String,

        @param:JacksonXmlProperty(localName = "file")
        val file: String,

        @param:JacksonXmlElementWrapper(useWrapping = false)
        @param:JacksonXmlProperty(localName = "license")
        val licenses: List<License>,
    )

    class License(

        @param:JacksonXmlProperty(localName = "name", isAttribute = true)
        val name: String,

        @param:JacksonXmlProperty(localName = "url", isAttribute = true)
        val url: String?,
    )
}
