package com.recommendatorclient

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.recommendatorclient.databinding.FragmentSearchBinding
import com.recommendatorclient.recommendation_service.ApiClient
import com.recommendatorclient.recommendation_service.models.Recommendation
import com.recommendatorclient.recommendator_activity.*
import io.ktor.client.*
import io.ktor.client.call.*
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
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null

    private lateinit var _viewModel: RecommendatorViewModel
    private lateinit var mAdapter: RecommendationsListViewAdapter

    private var apiClient = ApiClient()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        initClickListeners()
        initRecyclerView()

        return binding.root
    }

    fun initClickListeners() {
        _binding!!.btnSearch.setOnClickListener { v: View ->
            updateRecommendationsBarcode(v)
        }
        _binding!!.btnPing.setOnClickListener { v: View ->
            apiClient.pingRecommendationsService()
        }
        _binding!!.btnPingGoogle.setOnClickListener { v: View ->
            apiClient.pingGoogle()
        }
    }

    private fun initRecyclerView() {
        _viewModel = ViewModelProvider(this).get(RecommendatorViewModel::class.java)

        // recyclerView
        val rv = _binding?.rvRecommendations
        rv?.setHasFixedSize(true)
        // use a linear layout manager
        val mLayoutManager = LinearLayoutManager(activity?.applicationContext)
        rv?.layoutManager = mLayoutManager

        // Обработка нажатий
        rv?.addOnItemTouchListener(
            RecyclerTouchListener(context, rv, object : ClickListener {
                override fun onClick(view: View?, position: Int) {
                    val recommendation: RecommendationModel? = mAdapter.items.getOrNull(position)
                    if (recommendation?.url != null) {
                        showUrlPopup(recommendation.url!!)
                    }
                }

                override fun onLongClick(view: View?, position: Int) {}
            })
        )

        mAdapter = RecommendationsListViewAdapter(ArrayList<RecommendationModel>())
        rv?.adapter = mAdapter

        // buttons
        _binding?.btnSearch?.setOnClickListener { view ->
            updateRecommendationsBarcode(view)
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

        disableButtons()
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
                    var respRecommendations: ArrayList<Recommendation> = resp.body()

                    client.close()

                    val j2 = launch(Dispatchers.Main) {
                        Log.d("Track", "pong barcode" + "resp ${resp.status}")

                        var recommendations = ArrayList<RecommendationModel>()
                        if (respRecommendations != null) {
                            for (r in respRecommendations) {
                                recommendations.add(
                                    RecommendationModel(
                                        productName = r.articul,
                                        shopName = r.shopName,
                                        price = r.price.toString(),
                                        url = r.url,
                                    )
                                )
                            }
                        }
                        _viewModel.setRecommendations(recommendations)
                        mAdapter.setRecommendations(recommendations)
                        activateButtons()
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

    fun showUrlPopup(itemUrl: String) {
        val builder = AlertDialog.Builder(this.requireContext())
        builder
            .setTitle("Товар")
            .setMessage("Карточка товара в магазине")
            .setPositiveButton("Открыть ссылку в браузере") { dialog, id ->
                val browserIntent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(itemUrl));
                startActivity(browserIntent)
                dialog.cancel()
            }
            .setNegativeButton("Отмена") { dialog, id ->
                dialog.cancel()
            }


        builder.create()
        builder.show()
    }

    fun disableButtons() {
        _binding?.btnSearch?.isActivated = false
        _binding?.btnPing?.isActivated = false
        _binding?.btnPingGoogle?.isActivated = false
    }

    fun activateButtons() {
        _binding?.btnSearch?.isActivated = true
        _binding?.btnPing?.isActivated = true
        _binding?.btnPingGoogle?.isActivated = true
    }
}