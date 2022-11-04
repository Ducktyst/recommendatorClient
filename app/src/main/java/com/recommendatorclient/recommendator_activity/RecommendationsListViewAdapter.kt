package com.recommendatorclient.recommendator_activity

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.recommendatorclient.R

class RecommendationsListViewAdapter(var items: ArrayList<RecommendationModel>) :
    RecyclerView.Adapter<RecommendationsListViewAdapter.RecommendationViewHolder>() {
    private lateinit var lInflater: LayoutInflater

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class RecommendationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvName = itemView.findViewById<TextView>(R.id.tvProductName)!!
        val tvShopName = itemView.findViewById<TextView>(R.id.tvShopName)!!
        val tvPrice = itemView.findViewById<TextView>(R.id.tvPriceForPack)!!
//        val url = itemView.findViewById<TextView>(R.id.tvPriceForKg)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        lInflater = LayoutInflater.from(parent.context)
        val itemView: View = lInflater.inflate(R.layout.recommendation_item, parent, false)

        return RecommendationViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        val price = items[position].price?.toInt()?.div(100)
        holder.tvName.text = items[position].productName
        holder.tvShopName.text = "${(items[position].shopName)}"
        holder.tvPrice.text = "${price} руб."
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setRecommendations(newItems: ArrayList<RecommendationModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}