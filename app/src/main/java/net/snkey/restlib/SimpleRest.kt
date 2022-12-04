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

import net.snkey.restlib.SimpleRestImpl.SimpleBuilder
import net.snkey.restlib.model.Timeouts

/**
 * SimpleRest interface with methods to setup get, post or head HTTP methods
 */
interface SimpleRest {

    /**
     * Sets timeouts for HTTP client. You can use this method only before sending first request!
     *
     * @param timeouts HTTP client timeout preferences
     */
    fun setTimeouts(timeouts: Timeouts)

    /**
     * Change basic URL for all requests from value specified in the main constructor
     *
     * @param baseUrl basic URL, used as base path for all requests
     */
    fun setBaseUrl(baseUrl: String)

    /**
     * Set the GET HTTP method and path
     *
     * @param methodUrl method address. If not beginning from http then will added to baseUrl
     */
    fun get(methodUrl: String): SimpleBuilder

    /**
     * Set the POST HTTP method, path and sent data
     *
     * @param methodUrl method address. If not beginning from http then will added to baseUrl
     * @param data object of any type to passed as String or JSON (detect automatically)
     */
    fun post(methodUrl: String, data: Any? = null): SimpleBuilder

    /**
     * Set the HEAD HTTP method and path
     *
     * @param methodUrl method address. If not beginning from http then will added to baseUrl
     */
    fun head(methodUrl: String): SimpleBuilder
}
