package com.recommendatorclient.recommendation_service.models

/**
 *
 * @param content Изображение со штрикодом
 */
data class RecommendationsBody(

    /* Изображение со штрикодом */
    val content: kotlin.Array<kotlin.Byte>
) {
}