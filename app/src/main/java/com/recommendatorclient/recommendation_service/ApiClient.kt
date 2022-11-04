package com.recommendatorclient.recommendation_service

import android.util.Log
import android.widget.Toast
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class ApiClient(var host: String = "https://0e9c-95-68-232-253.eu.ngrok.io") {

    fun getRecommendationsBarcode(barcode: String) {
        Log.d("Track", "getRecommendationsBarcode start")
        val url = "$host/api/recommendations/{barcode}".replace("{barcode}", barcode)

        val coroutineScope = CoroutineScope(Dispatchers.Default)
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

    fun pingRecommendationsService() {
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

    fun pingGoogle() {
        val coroutineScope = CoroutineScope(Dispatchers.Default)

        val url = "https://google.com"

        coroutineScope.launch(Dispatchers.Default) {
            try {
                val job = launch(Dispatchers.IO) {
                    val client = HttpClient(CIO) {}

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