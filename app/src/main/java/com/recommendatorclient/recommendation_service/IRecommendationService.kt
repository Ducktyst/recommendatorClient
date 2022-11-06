package com.recommendatorclient.recommendation_service

import com.recommendatorclient.recommendator_activity.RecommendationModel
import java.io.File

interface IRecommendationService {
    fun recommendationsBarcodeGet(barcode: String): kotlin.Array<RecommendationModel>
    fun recommendationsPost(file: File): kotlin.Array<RecommendationModel>
    fun ping()
}