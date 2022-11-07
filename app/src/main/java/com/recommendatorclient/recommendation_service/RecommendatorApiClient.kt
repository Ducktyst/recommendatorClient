package com.recommendatorclient.recommendation_service

import android.R.attr.bitmap
import android.graphics.Bitmap
import android.util.Log
import com.recommendatorclient.recommendation_service.models.Recommendation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer


class RecommendatorApiClient(var host: String = "https://08b3-87-76-11-149.eu.ngrok.io") {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20 * 60000 // N*min
            socketTimeoutMillis = 20 * 60000
            connectTimeoutMillis = 20 * 60000
        }
        engine {
            requestTimeout = 0 // 0 to disable, or a millisecond value to fit your needs
        }
        install(ContentNegotiation) {
            gson()
        }
    }
    val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun recommendationsBarcodeGet(barcode: String) {
        Log.d("Track", "getRecommendationsBarcode start")
        val url = "$host/api/recommendations/{barcode}".replace("{barcode}", barcode)

        try {
            coroutineScope.launch(Dispatchers.Default) {
                val job = launch(Dispatchers.IO) {
                    val client = HttpClient(CIO) {
                        install(HttpTimeout) {
                            requestTimeoutMillis = 20 * 60000 // N*min
                            socketTimeoutMillis = 20 * 60000
                            connectTimeoutMillis = 20 * 60000
                        }
                        engine {
                            requestTimeout = 0 // 0 to disable, or a millisecond value to fit your needs
                        }
                        install(ContentNegotiation) {
                            gson()
                        }
                    }

                    val resp: HttpResponse = client.get(url)
                    // deserialize to ArrayList<Recommendation>

                    client.close()

                    val j2 = launch(Dispatchers.Main) {
                        Log.d("Track", "pong barcode" + "resp ${resp.status}")

//                        var recommendations = ArrayList<Recommendation>()
//                        if (respRecommendations != null) {
//                            for (r in respRecommendations) {
//                                recommendations.add(
//                                    Recommendation(
//                                        productName = r.articul,
//                                        shopName = "shopName",
//                                        price = r.price.toString(),
//                                        url = r.url,
//                                    )
//                                )
//                            }
//                        }
//                        _viewModel.setRecommendations(recommendations)
//                        mAdapter.setRecommendations(recommendations)
                    }
                    j2.join()
                }

                job.join()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("Track", "getRecommendationsBarcode ex $e")
        }

        Log.d("Track", "getRecommendationsBarcode finish")
    }

    fun ping() {
        var coroutineScope = CoroutineScope(Dispatchers.Default)
        var url = "$host/api/ping"

        try {
            coroutineScope.launch(Dispatchers.Default) {
                val job = launch(Dispatchers.IO) {
                    val client = HttpClient(CIO) {
                        install(ContentNegotiation) {
                            gson()
                        }
                    }

                    val resp: HttpResponse = client.get(url)
                    client.close()

                    val j2 = launch(Dispatchers.Main) {
                        Log.d("Track", "pong")
                    }
                    j2.join()
                }

                job.join()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d("Track", "pingRecommendationsService finish")
    }

    @OptIn(InternalAPI::class)
    suspend fun recommendationsPost(img: Bitmap): ArrayList<Recommendation> {
        // TOOD: error handling
        val url = "$host/api/recommendations"

        val byteArray = bitmapToByteArray(img)

        Log.d("Track", "$url")
        val resp: HttpResponse = client.request {
            url(url)
            method = HttpMethod.Post
            body = MultiPartFormDataContent(
                formData {
                    append(
                        "content",
                        byteArray,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/png") // Mime type required
                            append(HttpHeaders.ContentDisposition, "filename=\"img.png\"")
                            append(HttpHeaders.Accept, "application/json")
                        }
                    )
                }
            )
        }
        Log.d("Track", "resp.status " + resp.status)
        Log.d("Track", "resp " + resp.bodyAsText())


        if (resp.status != HttpStatusCode.OK) {
            return ArrayList<Recommendation>() // TODO: throw err
        }

        val respRecommendations: ArrayList<Recommendation> = resp.body()
        return respRecommendations
    }

    private fun bitmapToByteArray(img: Bitmap): ByteArray {
        val size: Int = img.getRowBytes() * img.getHeight()
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(size)
        img.copyPixelsToBuffer(byteBuffer)
        val byteArray = byteBuffer.array()

        return byteArray

    }


    fun pingGoogle() {

        val url = "https://google.com"

        coroutineScope.launch(Dispatchers.Default) {
            try {
                val job = launch(Dispatchers.IO) {
                    val resp: HttpResponse = client.get(url)
                    client.close()

                    val j2 = launch(Dispatchers.Main) {
                        Log.d("Track", "pong google")
                    }
                    j2.join()
                }

                job.join()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}