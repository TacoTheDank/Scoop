package taco.scoop.util

import android.content.Context
import org.json.JSONObject
import taco.scoop.R
import java.net.HttpURLConnection
import java.net.URL

class PasteException(message: String) : Exception(message)

fun createPaste(context: Context, text: String): String {
    val url = PreferenceHelper.pasteUrl
        ?: throw PasteException(context.getString(R.string.setup_paste_service))

    val template = PreferenceHelper.pasteTemplate ?: "$url/%s"

    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.addRequestProperty("User-Agent", "Scoop (https://github.com/TacoTheDank/Scoop)")
    connection.addRequestProperty("Content-Type", "text/plain")
    connection.addRequestProperty("Accept", "application/json")
    connection.doOutput = true
    connection.outputStream.use {
        it.write(text.toByteArray())
    }

    val res = connection.inputStream.use {
        it.bufferedReader().readText()
    }

    val key = JSONObject(res).getString(PreferenceHelper.pasteResponseJsonKey ?: "key")
    return template.format(key)
}
