package com.recommendatorclient

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.recommendatorclient.databinding.FragmentSearchBinding
import com.recommendatorclient.recommendation_service.RecommendatorApiClient
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
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null

    private lateinit var _viewModel: RecommendatorViewModel
    private lateinit var mAdapter: RecommendationsListViewAdapter

    private var apiClient = RecommendatorApiClient()

    private lateinit var startForResult: ActivityResultLauncher<Intent>

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

        _binding?.imageView?.isEnabled = false

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
        checkCameraPersimiision()
        startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                Log.d("Track", "recommendationsPost start")
                if (result.resultCode != Activity.RESULT_OK) {
                    Log.d("Track", "camera.resultCode != RESULT_OK")
                }
                if (result.data == null) {
                    return@registerForActivityResult
                }

                val bitmapImg = result.data?.extras?.get("data") as Bitmap

                _binding?.imageView?.load(bitmapImg) {
                    crossfade(true)
                    crossfade(1000)
                    transformations(CircleCropTransformation())
                }
                _binding?.imageView?.isEnabled = true

                // request
                val coroutineScope = CoroutineScope(Dispatchers.Default)
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        Log.d("Track", "recommendationsPost start request")
                        val respRecommendations: ArrayList<Recommendation> = apiClient.recommendationsPost(bitmapImg)
                        Log.d("response", "recommendationsPost end request")

                        coroutineScope.launch(Dispatchers.Main) {
                            applyRecommendations(respRecommendations)
                        }
                    } catch (e: NoTransformationFoundException) {
                        e.printStackTrace()
                    } finally {
                        coroutineScope.launch(Dispatchers.Main) {
                            enableButtons()
                        }
                    }

                }
                Log.d("Track", "recommendationsPost end")


            }
        _binding!!.btnSearchByPhoto.setOnClickListener { v: View ->
            postRecommendationsImage(v)
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

        val host: String = "https://08b3-87-76-11-149.eu.ngrok.io" // TODO: ...
        val url = "$host/api/recommendations/{barcode}".replace("{" + "barcode" + "}", barcode)

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
                    // deserialize to ArrayList<Recommendation>
                    Log.d(
                        "Track",
                        "resp ${url.replace(host, "")} ${resp.status} ${resp.bodyAsText().subSequence(0, 30)}"
                    )
                    val respRecommendations: ArrayList<Recommendation> = resp.body()
//                    client.close()
                    val recommendations = ArrayList<RecommendationModel>()
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

                    val j2 = launch(Dispatchers.Main) {
                        Log.d("Track", "pong barcode" + "resp ${resp.status}")
                        _viewModel.setRecommendations(recommendations)
                        mAdapter.setRecommendations(recommendations)
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

    fun getRecommendationsBarcode2(barcode: String) {
        Log.d("Track", "getRecommendationsBarcode start")

        val url = "$host/api/recommendations/{barcode}".replace("{" + "barcode" + "}", barcode)

        disableButtons()
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        runBlocking {

            try {
                val reqJob = coroutineScope.async(Dispatchers.Default) {
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
                            applyRecommendations(respRecommendations)
                            enableButtons()
                        }
                        j2.join()

                    }

                    job.join()
                    return@async null
                }
                val empty = reqJob.await()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("Track", "getRecommendationsBarcode ex $e")
            }
        }
        Log.d("Track", "getRecommendationsBarcode finish")
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
        _viewModel.setRecommendations(recommendations)
        mAdapter.setRecommendations(recommendations)
    }

    fun postRecommendationsImage(v: View?) {
        disableButtons()

        val file = File("path/to/some.file")
        val chatId = "123"
        val img = camera()
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

    private fun checkCameraPersimiision() {
        Dexter.withContext(this.context)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            ).withListener(
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if (report.areAllPermissionsGranted()) {
//                                camera()
                            } else {
                                showRotationalDialogForPermission()
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRotationalDialogForPermission()
                    }
                }
            ).onSameThread().check()
    }

    private fun camera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startForResult.launch(intent)
    }

    private fun showRotationalDialogForPermission() {
        AlertDialog.Builder(this.requireContext())
            .setMessage(
                "Похоже, что вы отключили разрешения необходимые для этой возможности." +
                        "Их можно включить в системных настройках для приложения!"
            ).setPositiveButton("Открыть настройки") { dialog, id ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", activity?.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Отмеена") { dialog, id ->
                dialog.dismiss()
            }.show()
    }
}