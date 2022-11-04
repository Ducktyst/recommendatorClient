package com.recommendatorclient

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.recommendatorclient.databinding.FragmentFirstBinding
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

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        initClickListeners()
        return binding.root
    }

    fun initClickListeners() {
        _binding!!.btnSearch.setOnClickListener { v: View ->
            updateRecommendationsBarcode(v)
        }
        _binding!!.btnPing.setOnClickListener { v: View ->
            pingRecommendationsService()
        }
        _binding!!.btnPingGoogle.setOnClickListener { v: View ->
            pingGoogle()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateRecommendationsBarcode(v: View?) {
        val barcode = _binding?.etBarcode?.text.toString()
        if (barcode == "") {
            return
        }
        getRecommendationsBarcode(barcode)

    }

    fun getRecommendationsBarcode(barcode: String) {
        Log.d("Track", "getRecommendationsBarcode start")

        val host: String = "https://0e9c-95-68-232-253.eu.ngrok.io" // TODO: ...
        val url = "$host/api/recommendations/{barcode}".replace("{" + "barcode" + "}", barcode)

        val coroutineScope = CoroutineScope(Dispatchers.Default)
        try {
            coroutineScope.launch(Dispatchers.Default) {
                val j1 = launch(Dispatchers.Main) {
                    Toast.makeText(context, "call $url", Toast.LENGTH_LONG).show()
                }
                j1.join()

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

                    client.close()

                    val j2 = launch(Dispatchers.Main) {
                        Log.d("Track", "pong")
                        Toast.makeText(context, "resp ${resp.status}", Toast.LENGTH_LONG).show()
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
        Log.d("Track", "pingRecommendationsService start")
        val host: String = "https://0e9c-95-68-232-253.eu.ngrok.io" // TODO: ...

        var coroutineScope = CoroutineScope(Dispatchers.Default)

        var url = "$host/api/ping"

        try {
            coroutineScope.launch(Dispatchers.Default) {
                val j1 = launch(Dispatchers.Main) {
                    Toast.makeText(context, "ping api $url", Toast.LENGTH_LONG).show()
                }
                j1.join()

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
                        Toast.makeText(context, "pong ${resp.status}", Toast.LENGTH_LONG).show()
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
                val j1 = launch(Dispatchers.Main) {
                    Log.d("Track", "ping google")

                    Toast.makeText(context, "ping api $url", Toast.LENGTH_LONG).show()
                }
                j1.join()

                val job = launch(Dispatchers.IO) {
                    val client = HttpClient(CIO) {}

                    val resp: HttpResponse = client.get(url)

                    client.close()

                    val j2 = launch(Dispatchers.Main) {
                        Log.d("Track", "pong google")
                        Toast.makeText(context, "pong google ${resp.status}", Toast.LENGTH_LONG).show()
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