package logging.appender

import logging.LogLevel
import java.io.File
import java.io.IOException

class XmlAppender(
    private val filePath: String
) : Appender {

    private val file = File(filePath)

    override fun append(level: LogLevel, message: String, throwable: Throwable?) {
        val xmlLog = buildString {
            append("<log>\n")
            append("  <timestamp>${System.currentTimeMillis()}</timestamp>\n")
            append("  <level>${level.name}</level>\n")
            append("  <message>${escapeXml(message)}</message>\n")
            throwable?.let {
                append("  <exception>${escapeXml(it.stackTraceToString())}</exception>\n")
            }
            append("</log>\n")
        }
        try {
            file.appendText(xmlLog)
        } catch (e: IOException) {
            println("Error writing to XML log file: ${e.message}")
        }
    }

    

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}