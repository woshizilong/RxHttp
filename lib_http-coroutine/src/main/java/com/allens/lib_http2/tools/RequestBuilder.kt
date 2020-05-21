package com.allens.lib_http2.tools

import com.allens.lib_http2.core.HttpResult
import com.allens.lib_http2.impl.ApiService
import com.allens.lib_http2.manager.HttpManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.*


class RequestBuilder {

    private val heard = HashMap<String, String>()
    private val map = HashMap<String, Any>()
    private val bodyMap = HashMap<String, RequestBody>()

    fun addHeard(key: String, value: String): RequestBuilder {
        heard[key] = value
        return this
    }

    fun addParameter(key: String, value: Any): RequestBuilder {
        map[key] = value
        return this
    }


    fun addRequestBody(key: String, path: String): RequestBuilder {
        val fileBody: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), File(path))
        bodyMap[key] = fileBody
        return this
    }


    suspend fun <T : Any> doGet(
        parameter: String,
        tClass: Class<T>
    ): HttpResult<T> {
        return executeResponse(
            {
                val baseUrl = HttpManager.retrofit.baseUrl().toUrl().toString()
                var getUrl: String = baseUrl + parameter
                if (map.size > 0) {
                    val param: String = UrlTool.prepareParam(map)
                    if (param.trim().isNotEmpty()) {
                        getUrl += "?$param"
                    }
                }
                HttpManager.getService(ApiService::class.java)
                    .doGet(heard, getUrl)
                    .body()
                    ?.string()
            }, tClass
        )
    }

    suspend fun <T : Any> doPost(parameter: String, tClass: Class<T>): HttpResult<T> {
        return executeResponse({
            HttpManager.getService(ApiService::class.java)
                .doPost(parameter, heard, map).body()
                ?.string()
        }, tClass)
    }


    suspend fun <T : Any> doBody(parameter: String, tClass: Class<T>): HttpResult<T> {
        return executeResponse({
            val toJson = HttpManager.gson.toJson(map)
            val requestBody =
                toJson.toRequestBody("application/json".toMediaTypeOrNull())
            HttpManager.getService(ApiService::class.java)
                .doBody(parameter, heard, requestBody)
                .body()
                ?.string()
        }, tClass)
    }


    suspend fun <T : Any> doDelete(parameter: String, tClass: Class<T>): HttpResult<T> {
        return executeResponse({
            HttpManager.getService(ApiService::class.java)
                .doDelete(parameter, heard, map).body()
                ?.string()
        }, tClass)
    }

    suspend fun <T : Any> doPut(parameter: String, tClass: Class<T>): HttpResult<T> {
        return executeResponse({
            HttpManager.getService(ApiService::class.java)
                .doPut(parameter, heard, map).body()
                ?.string()
        }, tClass)
    }


    suspend fun <T : Any> doUpload(parameter: String, tClass: Class<T>): HttpResult<T> {
        return executeResponse({
            HttpManager.getService(ApiService::class.java)
                .upload(parameter, heard, bodyMap).body()
                ?.string()
        }, tClass)
    }

    private suspend fun <T : Any> executeResponse(
        call: suspend () -> String?,
        tClass: Class<T>
    ): HttpResult<T> {
        return try {
            HttpResult.Success(HttpManager.gson.fromJson(call(), tClass))
        } catch (e: Throwable) {
            HttpResult.Error(e)
        }
    }


}