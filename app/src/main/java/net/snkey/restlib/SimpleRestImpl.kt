/*
 * Copyright (C) 2022 Serge Korzhinsky (serge@snkey.net)
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

import net.snkey.restlib.model.SimpleResponse
import net.snkey.restlib.model.Timeouts
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request.Builder
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.IOException
import java.lang.reflect.Type

/**
 * SimpleRest implementation
 *
 * @property baseUrl basic URL, used as base path for all requests. You can set it also with [setBaseUrl] method
 * @property timeouts HTTP client timeout preferences. You can set it also with [setTimeouts] method before first call
 */
class SimpleRestImpl(
    private var baseUrl: String = "",
    private var timeouts: Timeouts = Timeouts()
) : SimpleRest {

    /** Setup OkHttp3 client */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(timeouts.connect, timeouts.unit)
            .writeTimeout(timeouts.write, timeouts.unit)
            .readTimeout(timeouts.read, timeouts.unit)
            .callTimeout(timeouts.total, timeouts.unit)
            .build()
    }

    /** Request builder */
    private val requestBuilder = SimpleBuilder()

    /** Stores request URI  */
    private var requestUrl = ""

    /** All possible response types based on HTTP code */
    private val responseTypes = mutableMapOf<Int, Type>()

    /** Request parameters */
    private val params = mutableMapOf<String, String>()

    override fun setBaseUrl(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    override fun setTimeouts(timeouts: Timeouts) {
        this.timeouts = timeouts
    }

    override fun get(methodUrl: String): SimpleBuilder {
        reset()
        requestBuilder.get()
        requestUrl = methodUrl
        return requestBuilder
    }

    override fun post(methodUrl: String, data: Any?): SimpleBuilder {
        reset()
        data?.let {
            val body = makePostedBody(it)
            requestBuilder.post(body)
        }
        requestUrl = methodUrl
        return requestBuilder
    }

    override fun head(methodUrl: String): SimpleBuilder {
        reset()
        requestBuilder.head()
        requestUrl = methodUrl
        return requestBuilder
    }

    /** Prepare request to execute */
    private fun buildCall() = client.newCall(buildUrl().build())

    /** Execute request and return response */
    private fun <T> getResponse(classOfT: Class<T>): SimpleResponse<T?> {
        try {
            val response = buildCall().execute()
            return SimpleResponse(
                body = mapResponseBody(classOfT, response.body, response.code),
                code = response.code,
                headers = response.headers,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw IOException("Call fails")
        }
    }

    /** Prepare request body with correct type */
    private fun makePostedBody(data: Any): RequestBody = when (data) {
        is String -> data.toRequestBody(TEXT_MEDIA_TYPE.toMediaType())
        is Number -> data.toString().toRequestBody(TEXT_MEDIA_TYPE.toMediaType())
        is Boolean -> data.toString().toRequestBody(TEXT_MEDIA_TYPE.toMediaType())
        else -> Gson().toJson(data).toRequestBody(JSON_MEDIA_TYPE.toMediaType())
    }

    /** Maps response body to corresponded type */
    @Suppress("UNCHECKED_CAST")
    private fun <T> mapResponseBody(classOfT: Class<T>, body: ResponseBody?, code: Int): T? {
        val type = responseTypes.getOrDefault(code, classOfT as Type)
        if (code != 200 && type == classOfT as Type) return null
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

    /** Builds URL from all specified parts */
    private fun buildUrl(): SimpleBuilder {
        var ps = if (baseUrl.isNotBlank() && !requestUrl.startsWith("http"))
            "$baseUrl$requestUrl?" else "$requestUrl?"
        params.forEach {
            ps += it.key + "=" + it.value + "&"
        }
        if (ps.endsWith('&') || ps.endsWith('?')) ps = ps.dropLast(1)
        requestBuilder.url(ps)
        return requestBuilder
    }

    /** Clear params and response types before call new request */
    private fun reset() {
        params.clear()
        responseTypes.clear()
    }

    /**
     * Request builder. Used to setup and to call request.
     */
    inner class SimpleBuilder : Builder() {

        /**
         * Adds additional response type based on HTTP response code.
         * You can add type to each HTTP code except 200 (it is specified in getResponse).
         *
         * @param httpCode HTTP code to assign type
         * @param classOfT type assigned to response with specified HTTP code
         */
        fun <T> addResponseType(httpCode: Int, classOfT: Class<T>): SimpleBuilder {
            responseTypes[httpCode] = classOfT
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
         * Execute request and return response
         *
         * @param classOfT type for default success response (for code 200)
         */
        fun <T> getResponse(classOfT: Class<T>): SimpleResponse<T?> =
            this@SimpleRestImpl.getResponse(classOfT)
    }

    private companion object {

        /** Supported media types for HTTP POST method */
        const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        const val TEXT_MEDIA_TYPE = "text/plain; charset=utf-8"

        const val TYPE_TEXT = "text"
        const val TYPE_JSON = "json"
    }
}