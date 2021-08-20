package com.facedetector.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.facedetector.DemoListActivity
import com.facedetector.R

class DemosAdapter(private val dataset: Array<Demo>) :
    RecyclerView.Adapter<DemosAdapter.ViewHolder>() {

    data class Demo(
        val title: String,
        val description: String,
        val layout: Int = 0,
        val activity: Class<*>
    ) {
        constructor(title: String, description: String, activity: Class<*>) : this(
            title,
            description,
            0,
            activity
        )
    }

    class ViewHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout) {
        var title = layout.findViewById(R.id.title) as TextView
        var description = layout.findViewById(R.id.description) as TextView
        var layoutFileId = 0
        var activity: Class<*>? = null

        init {
            layout.setOnClickListener { view ->
                val context = view?.context as DemoListActivity
                activity?.let {
                    context.start(it, layoutFileId)
                }
            }
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val row = LayoutInflater.from(parent.context)
            .inflate(R.layout.row, parent, false) as ConstraintLayout
        return ViewHolder(row)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = dataset[position].title
        holder.description.text = dataset[position].description
        holder.layoutFileId = dataset[position].layout
        holder.activity = dataset[position].activity
    }

    override fun getItemCount() = dataset.size
}