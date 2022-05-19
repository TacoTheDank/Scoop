package taco.scoop.util

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PasteException(message: String) : Exception(message) {}

fun createPaste(text: String): String {
    val url = PreferenceHelper.pasteUrl
        ?: throw PasteException("Setup a Paste Service in Settings and try again")

    val template = PreferenceHelper.pasteTemplate ?: "$url/%s"

    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.addRequestProperty("User-Agent", "Scoop (https://github.com/TacoTheDank/Scoop)")
    conn.addRequestProperty("Content-Type", "text/plain")
    conn.addRequestProperty("Accept", "application/json")
    conn.doOutput = true
    conn.outputStream.use {
        it.write(text.toByteArray())
    }

    val res = conn.inputStream.use {
        it.bufferedReader().readText()
    }

    val key = JSONObject(res).getString(PreferenceHelper.pasteResponseJsonKey ?: "key")
    return template.format(key)
}
