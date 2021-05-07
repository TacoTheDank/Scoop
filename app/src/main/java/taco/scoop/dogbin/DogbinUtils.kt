/*
 * Copyright (C) 2018 Potato Open Sauce Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package taco.scoop.dogbin

import android.os.Handler
import android.os.HandlerThread
import android.util.JsonReader
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

/**
 * Helper functions for uploading to del.dog
 */
object DogbinUtils {
    private const val TAG = "DogbinUtils"
    private const val BASE_URL = "https://del.dog"
    private val API_URL = String.format("%s/documents", BASE_URL)
    private var handler: Handler? = null
        get() {
            if (field == null) {
                val handlerThread = HandlerThread("dogbinThread")
                if (!handlerThread.isAlive) {
                    handlerThread.start()
                }
                field = Handler(handlerThread.looper)
            }
            return field
        }

    /**
     * Uploads `content` to dogbin
     *
     * @param content  the content to upload to dogbin
     * @param callback the callback to call on success / failure
     */
    fun upload(content: String?, callback: UploadResultCallback) {
        handler!!.post {
            try {
                val urlConnection = URL(API_URL).openConnection() as HttpsURLConnection
                try {
                    urlConnection.setRequestProperty("Accept-Charset", "UTF-8")
                    urlConnection.doOutput = true
                    urlConnection.outputStream.use { output ->
                        output.write(
                            content?.toByteArray(
                                StandardCharsets.UTF_8
                            )
                        )
                    }
                    var key = ""
                    JsonReader(
                        InputStreamReader(urlConnection.inputStream, StandardCharsets.UTF_8)
                    ).use { reader ->
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val name = reader.nextName()
                            if (name == "key") {
                                key = reader.nextString()
                                break
                            }
                            reader.skipValue()
                        }
                        reader.endObject()
                    }
                    if (key.isNotEmpty()) {
                        callback.onSuccess(getUrl(key))
                    } else {
                        val msg = "Failed to upload to dogbin: No key retrieved"
                        callback.onFail(msg, DogbinException(msg))
                    }
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: Exception) {
                callback.onFail("Failed to upload to dogbin", e)
            }
        }
    }

    /**
     * Get the view URL from a key
     */
    private fun getUrl(key: String): String {
        return String.format("%s/%s", BASE_URL, key)
    }

    interface UploadResultCallback {
        fun onSuccess(url: String)
        fun onFail(message: String, e: Exception)
    }
}
