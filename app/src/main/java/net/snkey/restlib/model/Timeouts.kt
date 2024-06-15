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
package net.snkey.restlib.model

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Request timeouts.
 *
 * @property connect default connect timeout for new connections, 0 means no limit
 * @property write default write timeout for new connections, 0 means no limit
 * @property read default read timeout for new connections, 0 means no limit
 * @property total default timeout for complete calls, 0 means no limit
 * @property unit time unit to set all connections, seconds by default
 */
data class Timeouts(
    val connect: Long = DEFAULT_TIMEOUT,
    val write: Long = DEFAULT_TIMEOUT,
    val read: Long = DEFAULT_TIMEOUT,
    val total: Long = UNLIMITED_TIMEOUT,
    val unit: TimeUnit = SECONDS
) {

    private companion object {

        const val DEFAULT_TIMEOUT = 15L
        const val UNLIMITED_TIMEOUT = 0L
    }
}