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
package net.snkey.restlib.util

import net.snkey.restlib.SimpleRestImpl.SimpleBuilder
import net.snkey.restlib.model.SimpleResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Extension to call request as suspended function in the coroutine context.
 *
 * @param classOfT type of required class
 * @return [SimpleResponse] object with response data
 */
suspend fun <T> SimpleBuilder.awaitResponse(
    classOfT: Class<T>
): SimpleResponse<T?> {
    return withContext(Dispatchers.IO) {
        val response = getResponse(classOfT)
        @Suppress("UNCHECKED_CAST")
        (response as? SimpleResponse<T?>)
            ?: throw IllegalArgumentException("You specified incorrect response type")
    }
}

/**
 * Simplified extension to call request as suspended function in coroutine context.
 *
 * @return [SimpleResponse] object with response data
 */
suspend inline fun <reified T> SimpleBuilder.awaitResponse(): SimpleResponse<T?> =
    awaitResponse(T::class.java)

/**
 * Simplified extension to call request as suspended function in coroutine context.
 *
 * @return only response data of type [T]
 */
suspend inline fun <reified T> SimpleBuilder.awaitData(): T? =
    awaitResponse(T::class.java).body

/**
 * Extension to call request with specified interval.
 *
 * @param classOfT type of required class
 * @param interval delay between requests
 * @return cold flow of type [T]
 */
fun <T> SimpleBuilder.toFlow(
    classOfT: Class<T>,
    interval: Long
): Flow<T?> = flow {
    while (true) {
        val response = getResponse(classOfT)
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
    toFlow(T::class.java, interval)