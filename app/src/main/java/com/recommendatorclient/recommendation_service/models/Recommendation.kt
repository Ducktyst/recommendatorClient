package com.recommendatorclient.recommendation_service.models


/**
 *
 * @param articul
 * @param shopName
 * @param price
 * @param barcode
 * @param url
 */
data class Recommendation(

    val articul: kotlin.String? = null,
    val shopName: kotlin.String? = null,
    val price: java.math.BigDecimal? = null,
    val barcode: kotlin.String? = null,
    val url: kotlin.String? = null
) {
}