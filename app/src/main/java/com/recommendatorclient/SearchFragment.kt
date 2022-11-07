package com.recommendatorclient

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.recommendatorclient.databinding.FragmentSearchBinding
import com.recommendatorclient.recommendation_service.RecommendatorApiClient
import com.recommendatorclient.recommendation_service.models.Recommendation
import com.recommendatorclient.recommendator_activity.*
import com.recommendatorclient.recommendator_activity.CameraComponent.CameraHQ
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
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

lateinit var photoFile: File

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null

    private lateinit var _viewModel: RecommendatorViewModel
    private lateinit var mAdapter: RecommendationsListViewAdapter

    private var apiClient = RecommendatorApiClient()

    private lateinit var startForResult: ActivityResultLauncher<Intent>

    private var mCurrentPhotoPath: String = ""

    private lateinit var cameraHQ: CameraHQ

    private val FILE_NAME = "barcode_photo"
    private val CAMERA_REQ_CODE = 1

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        cameraHQ = CameraHQ(this.requireActivity(), apiClient)

        initClickListeners()
        initRecyclerView()
//        _binding?.imageView?.isEnabled = false

        return binding.root
    }

    fun initClickListeners() {
        _binding!!.btnSearch.setOnClickListener { v: View ->
            updateRecommendationsBarcode(v)
        }
        _binding!!.btnPing.setOnClickListener { v: View ->
            apiClient.ping()
        }
        _binding!!.btnPingGoogle.setOnClickListener { v: View ->
            apiClient.pingGoogle()
        }

        // camera
//        cameraHQ.checkCameraPermission()

        _binding!!.btnSearchByPhoto.setOnClickListener { v: View ->
            cameraHQ.onTakePhotoClick(startForResult)
        }

        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
//            val takenImage = cameraHQ.getTakenPhoto()
//            _binding?.imageView?.isEnabled = true

            cameraIntentOnResult(result)
            return@registerForActivityResult
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == CAMERA_REQ_CODE && resultCode == Activity.RESULT_OK) {
//            val takenImage = data?.extras?.get("data") as Bitmap
//            val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
//            binding.imageView.setImageBitmap(takenImage)
//        } else {
//            super.onActivityResult(requestCode, resultCode, data)
//        }
//
//    }

    private fun cameraIntentOnResult(result: ActivityResult) {
        Log.d("Track", "recommendationsPost start")
        if (result.resultCode != Activity.RESULT_OK) {
            Log.d("Track", "camera.resultCode != RESULT_OK")
            return
        }

        val bitmapImg = cameraHQ.getTakenPhoto()
        if (bitmapImg == null) {
            Log.d("Track", "bitmapImg == null")
            return
        }

        val coroutineScope = CoroutineScope(Dispatchers.Default)
        try {
            // request
            coroutineScope.launch(Dispatchers.IO) {
                Log.d("Track", "recommendationsPost start request")
                val respRecommendations: ArrayList<Recommendation> = apiClient.recommendationsPost(bitmapImg)
                Log.d("response", "recommendationsPost end request")

                coroutineScope.launch(Dispatchers.Main) {
                    applyRecommendations(respRecommendations)
                }
            }
            Log.d("Track", "recommendationsPost end")
        } catch (e: FileNotFoundException) {
            e.printStackTrace() // TODO Auto-generated catch block
        } catch (e: NoTransformationFoundException) {
            e.printStackTrace()
        } finally {
            coroutineScope.launch(Dispatchers.Main) {
                enableButtons()
            }
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

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }
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
        // TODO: move to apiClient
        getRecommendationsBarcode(barcode)
    }

    fun getRecommendationsBarcode(barcode: String) {
        Log.d("Track", "getRecommendationsBarcode start")
        val url = "${apiClient.host}/api/recommendations/{barcode}".replace("{" + "barcode" + "}", barcode)

        disableButtons()
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        try {
            coroutineScope.launch(Dispatchers.Main) {
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
                    Log.d(
                        "Track",
                        "resp ${url.replace(apiClient.host, "")} ${resp.status} ${resp.bodyAsText().subSequence(0, 30)}"
                    )
                    // deserialize to ArrayList<Recommendation>
                    val respRecommendations: ArrayList<Recommendation> = resp.body()
                    client.close()

                    val j2 = launch(Dispatchers.Main) {
                        Log.d("Track", "pong barcode" + "resp ${resp.status}")
                        applyRecommendations(respRecommendations)
                        enableButtons()
                    }
                    j2.join()
                }
                job.join()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("Track", "getRecommendationsBarcode ex $e")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.d("Track", "getRecommendationsBarcode finish")
    }

    fun showUrlPopup(itemUrl: String) {
        val builder = AlertDialog.Builder(this.requireContext())
        builder
            .setTitle("Товар")
            .setMessage("Карточка товара в магазине")
            .setPositiveButton("Открыть ссылку в браузере") { dialog, id ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(itemUrl)))
                dialog.cancel()
            }
            .setNegativeButton("Отмена") { dialog, id ->
                dialog.cancel()
            }

        builder.create()
        builder.show()
    }

    private fun disableButtons() {
        _binding?.btnSearch?.isActivated = false
        _binding?.btnPing?.isActivated = false
        _binding?.btnPingGoogle?.isActivated = false
    }

    private fun enableButtons() {
        _binding?.btnSearch?.isActivated = true
        _binding?.btnPing?.isActivated = true
        _binding?.btnPingGoogle?.isActivated = true
    }

    private fun applyRecommendations(respRecommendations: java.util.ArrayList<Recommendation>) {
        var recommendations = ArrayList<RecommendationModel>()
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
        mAdapter.setRecommendations(recommendations)
    }

}