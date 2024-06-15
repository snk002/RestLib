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
package net.snkey.restlib.util

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.snkey.restlib.SimpleRestImpl.SimpleBuilder
import net.snkey.restlib.model.SimpleResponse
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.IOException

/**
 * Extension to call request for generics as suspended function in the coroutine context.
 *
 * @param typeToken type of required class
 * @return [SimpleResponse] object with response data
 */
suspend fun <T> SimpleBuilder.awaitResponse(
    typeToken: TypeToken<T>,
): SimpleResponse<T?> {
    return withContext(Dispatchers.IO) {
        val response = getResponse(typeToken)
        (response as? SimpleResponse<T?>)?.also {
            if (it.code >= 500) throw IOException("${it.code}")
            else if (it.code >= 400) throw IllegalStateException("${it.code}")
        } ?: throw IllegalArgumentException("You specified incorrect response type")
    }
}

/**
 * Simplified extension to call request as suspended function in coroutine context.
 *
 * @return [SimpleResponse] object with response data
 */
suspend inline fun <reified T> SimpleBuilder.awaitResponse(): SimpleResponse<T?> {
    return awaitResponse(object : TypeToken<T>() {})
}

@SuppressWarnings
inline fun <reified T> SimpleBuilder.addResponseType(code: Int, classOfT: Class<T>): SimpleBuilder {
    return addResponseType(code, object : TypeToken<T>() {})
}

/**
 * Simplified extension to call request as suspended function in coroutine context.
 *
 * @return only response data of type [T]
 */
suspend inline fun <reified T> SimpleBuilder.awaitData(): T? =
    awaitResponse(object : TypeToken<T>() {}).body


/**
 * Extension to call request with specified interval.
 *
 * @param typeToken type of required class
 * @param interval delay between requests
 * @return cold flow of type [T]
 */
fun <T> SimpleBuilder.toFlow(
    typeToken: TypeToken<T>,
    interval: Long
): Flow<T?> = flow {
    while (true) {
        val response = getResponse(typeToken)
        emit(response.body)
        delay(interval)
    }
}.flowOn(Dispatchers.IO)
    .buffer(Channel.UNLIMITED)

/**
 * Simplified extension to call request with specified interval.
 *
 * @return cold flow of type [T]
 */
inline fun <reified T> SimpleBuilder.toFlow(interval: Long): Flow<T?> =
    toFlow(object : TypeToken<T>() {}, interval)


/**
 * Extension to save response body as is to the file with download progress state flow.
 * Use with SimpleRest.getRawResponse()
 *
 * @param fileName file name with full path
 * @return cold flow of type [DownloadState]
 */
fun Response.saveFile(fileName: String): Flow<DownloadState> {
    return flow {
        if (code != 200) {
            emit(DownloadState.Failed(Exception(code.toString())))
            return@flow
        }
        emit(DownloadState.Started)
        val destinationFile = File(fileName)
        try {
            body?.byteStream()?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    val totalBytes = body?.contentLength() ?: 0
                    if (totalBytes == 0L) return@flow
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var progressBytes = 0L
                    var bytes = inputStream.read(buffer)
                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        progressBytes += bytes
                        bytes = inputStream.read(buffer)
                        emit(
                            DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt())
                        )
                    }
                }
            }
            body?.closeQuietly()
            emit(DownloadState.Finished)
        } catch (e: Exception) {
            body?.closeQuietly()
            emit(DownloadState.Failed(e))
        }
    }
        .flowOn(Dispatchers.IO).distinctUntilChanged()
}

/**
 * Helpers interface for [Response.saveFile]
 */
sealed interface DownloadState {
    /**
     * This will emitted on start downloading
     */
    data object Started : DownloadState
    /**
     * This will emitted while downloading
     * @property progress percentage of completion
     */
    data class Downloading(val progress: Int) : DownloadState
    /**
     * This will emitted when download completes
     */
    data object Finished : DownloadState
    /**
     * This will emitted when downloading fails
     * @property error caught exception
     */
    data class Failed(val error: Throwable? = null) : DownloadState
}