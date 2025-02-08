/*
 * Copyright (C) 2022, 2024 Serge Korzhinsky (sergius.nicolaus@gmail.com)
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
package net.snkey.restlib

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.snkey.restlib.model.SimpleResponse
import net.snkey.restlib.model.Timeouts
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException

/**
 * SimpleRest implementation
 *
 * @property baseUrl basic URL, used as base path for all requests.
 * @property timeouts HTTP client timeout preferences.
 */
class SimpleRestImpl(
    private val baseUrl: String = "",
    private val timeouts: Timeouts = Timeouts()
) : SimpleRest {

    // Setup OkHttp3 client
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(timeouts.connect, timeouts.unit)
            .writeTimeout(timeouts.write, timeouts.unit)
            .readTimeout(timeouts.read, timeouts.unit)
            .callTimeout(timeouts.total, timeouts.unit)
            .build()
    }

    // Requests builders pool
    private val buildersPool = mutableListOf<SimpleBuilder>()

    // Default headers for all requests
    private val defaultHeaders = mutableMapOf<String, String>()

    override fun get(methodUrl: String): SimpleBuilder {
        getBuilder(methodUrl).apply {
            get()
            return this
        }
    }

    override fun post(methodUrl: String, data: Any?): SimpleBuilder {
        getBuilder(methodUrl).apply {
            // using empty string if no real data because of okhttp3 limitations
            val body = makePostedBody(data ?: "")
            post(body)
            return this
        }
    }

    override fun put(methodUrl: String, data: Any?): SimpleBuilder {
        getBuilder(methodUrl).apply {
            // using empty string if no real data because of okhttp3 limitations
            val body = makePostedBody(data ?: "")
            put(body)
            return this
        }
    }

    override fun head(methodUrl: String): SimpleBuilder {
        getBuilder(methodUrl).apply {
            head()
            return this
        }
    }

    override fun delete(methodUrl: String): SimpleBuilder {
        getBuilder(methodUrl).apply {
            delete()
            return this
        }
    }

    override fun setHeaders(headers: Map<String, String>) {
        headers.forEach { (k, v) ->
            this.defaultHeaders[k] = v
        }
    }

    // Prepare request body with correct type
    private fun makePostedBody(data: Any): RequestBody = when (data) {
        is String -> data.toRequestBody(TEXT_MEDIA_TYPE.toMediaType())
        is Number -> data.toString().toRequestBody(TEXT_MEDIA_TYPE.toMediaType())
        is Boolean -> data.toString().toRequestBody(TEXT_MEDIA_TYPE.toMediaType())
        else -> Gson().toJson(data).toRequestBody(JSON_MEDIA_TYPE.toMediaType())
    }

    private fun getBuilder(methodUrl: String): SimpleBuilder {
        SimpleBuilder(methodUrl).apply {
            buildersPool.add(this)
            return this
        }
    }

    /**
     * Request builder. Used to setup and to call request.
     */
    inner class SimpleBuilder(private val requestUrl: String) : Builder() {

        // Request's own extra headers
        private val headers = mutableMapOf<String, String>()

        // All possible response types based on HTTP code
        private val responseTypes = mutableMapOf<Int, TypeToken<*>>()

        // Request's parameters
        private val params = mutableMapOf<String, String>()

        /**
         * Adds additional response type based on HTTP response code.
         * You can add type to each HTTP code except 200 (it is specified in getResponse).
         *
         * @param httpCode HTTP code to assign type
         * @param type type assigned to response with specified HTTP code
         */
        fun <T> addResponseType(httpCode: Int, type: TypeToken<T>): SimpleBuilder {
            responseTypes[httpCode] = type
            return this
        }

        /**
         * Adds parameters to request.
         * Parameters added to URI after '?' and separated by '&'
         *
         * @param name parameter name
         * @param value parameter value
         */
        fun addParam(name: String, value: Any): SimpleBuilder {
            params[name] = value.toString()
            return this
        }

        /**
         * Adds HTTP header. If it same (by the name) as one of the defaults, it will be overridden
         *
         * @param name HTTP header name
         * @param value HTTP header value
         */
        fun addHeader(name: String, value: Any): SimpleBuilder {
            headers[name] = value.toString()
            return this
        }

        /**
         * Execute request and return response as is
         */
        fun getRawResponse(): Response {
            try {
                return buildCall().execute()
            } finally {
                buildersPool.remove(this)
            }
        }

        /**
         * Execute request and return response for generics
         */
        fun <T> getResponse(type: TypeToken<T>): SimpleResponse<T?> {
            try {
                val response = buildCall().execute()
                return SimpleResponse(
                    body = mapResponseBody(type, response.body, response.code),
                    code = response.code,
                    headers = response.headers,
                )
            } catch (e: Exception) {
                e.printStackTrace()
                throw IOException("Call fails")
            } finally {
                buildersPool.remove(this)
            }
        }

        // Set HTTP hearers before call
        private fun setHeaders(): SimpleBuilder {
            defaultHeaders.forEach { (k, v) ->
                this.header(k, v)
            }
            headers.forEach { (k, v) ->
                this.header(k, v)
            }
            return this
        }

        // Prepare request to execute
        private fun buildCall() = client.newCall(this.setHeaders().buildUrl().build())

        // Builds URL from all specified parts
        private fun buildUrl(): SimpleBuilder {
            var fullPath = if (baseUrl.isNotBlank() && !requestUrl.startsWith("http"))
                "$baseUrl$requestUrl?" else "$requestUrl?"
            params.forEach { (key, value) ->
                fullPath += "$key=$value&"
            }
            if (fullPath.endsWith('&') || fullPath.endsWith('?')) fullPath = fullPath.dropLast(1)
            this.url(fullPath)
            return this
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> mapResponseBody(typeToken: TypeToken<T>, body: ResponseBody?, code: Int): T? {
            val type = responseTypes.getOrDefault(code, typeToken) as TypeToken<T>
            if (code != 200 && type == typeToken) return null
            return body?.let {
                try {
                    val bodyString: String = body.string()
                    body.close()
                    when (body.contentType()?.subtype ?: TYPE_TEXT) {
                        TYPE_TEXT -> if (type == String::class.java) bodyString as T else null
                        TYPE_JSON -> Gson().fromJson(bodyString, type)
                        else -> bodyString as? T
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private companion object {

        /** Supported media types for HTTP POST method */
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        const val TEXT_MEDIA_TYPE = "text/plain; charset=utf-8"

        const val TYPE_TEXT = "text"
        const val TYPE_JSON = "json"
    }
}