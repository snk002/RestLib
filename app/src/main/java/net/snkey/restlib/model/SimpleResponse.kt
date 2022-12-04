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
package net.snkey.restlib.model

import okhttp3.Headers

/**
 * Server response model
 *
 * @property body response body casted to specified type
 * @property code HTTP response status code
 * @property headers HTTP response headers
 */
data class SimpleResponse<T>(
    val body: T,
    val code: Int,
    val headers: Headers,
)