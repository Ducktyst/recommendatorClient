package com.recommendatorclient.recommendator_activity

import androidx.lifecycle.ViewModel

class RecommendatorViewModel : ViewModel() {
    private var _recommendation: ArrayList<RecommendationModel> = ArrayList()

    fun setRecommendations(recommendations: ArrayList<RecommendationModel>) {
        _recommendation = recommendations
    }
}