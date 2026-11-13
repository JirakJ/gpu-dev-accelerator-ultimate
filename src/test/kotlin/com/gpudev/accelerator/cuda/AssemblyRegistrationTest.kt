package com.gpudev.accelerator.cuda

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AssemblyRegistrationTest {
    @Test
    fun `PTX and SASS descriptor registrations reference packaged classes`() {
        val descriptor = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/resources/META-INF/plugin.xml"))
        val languages = setOf("PTX", "CUDA_SASS")
        val tags = listOf("fileType", "lang.parserDefinition", "lang.syntaxHighlighterFactory")
        val registrations = tags.flatMap { tag ->
            val nodes = descriptor.getElementsByTagName(tag)
            (0 until nodes.length).map { nodes.item(it) as Element }
        }.filter { it.getAttribute("language") in languages }

        assertTrue(registrations.isNotEmpty())
        registrations.forEach { registration ->
            Class.forName(registration.getAttribute("implementationClass"), false, javaClass.classLoader)
        }
    }
}
