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

/**
 * SimpleRest interface with methods to setup get, post or head HTTP methods
 */
interface SimpleRest {

    /**
     * Sets the GET HTTP method and path
     *
     * @param methodUrl method address. If not beginning from http then will added to baseUrl
     */
    fun get(methodUrl: String): SimpleRestImpl.SimpleBuilder

    /**
     * Sets the POST HTTP method, path and sent data
     *
     * @param methodUrl method address. If not beginning from http then will added to baseUrl
     * @param data object of any type to passed as String or JSON (detect automatically)
     */
    fun post(methodUrl: String, data: Any? = null): SimpleRestImpl.SimpleBuilder

    /**
     * Sets the PUT HTTP method, path and sent data
     *
     * @param methodUrl method address. If not beginning from http then will added to baseUrl
     * @param data object of any type to passed as String or JSON (detect automatically)
     */
    fun put(methodUrl: String, data: Any? = null): SimpleRestImpl.SimpleBuilder

    /**
     * Sets the HEAD HTTP method and path
     *
     * @param methodUrl method address. If not beginning from http then will added to baseUrl
     */
    fun head(methodUrl: String): SimpleRestImpl.SimpleBuilder

    /**
     * Sets the DELETE HTTP method and path
     *
     * @param methodUrl method address. If not beginning from http then will added to baseUrl
     */
    fun delete(methodUrl: String): SimpleRestImpl.SimpleBuilder

    /**
     * Sets default HTTP hearers for all requests.
     *
     * @param headers map of headers where key is header's name and value is header's value
     */
    fun setHeaders(headers: Map<String, String>)
}
