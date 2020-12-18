package de.koenidv.sph.adapters

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.koenidv.sph.R
import de.koenidv.sph.objects.Tile

//  Created by koenidv on 18.12.2020.
class LinksAdapter(private val dataset: List<Tile>, private val onClick: (Tile) -> Unit) :
        RecyclerView.Adapter<LinksAdapter.ViewHolder>() {

    /**
     * Provides a reference to the type of view
     * (custom ViewHolder).
     */
    class ViewHolder(view: View, val onClick: (Tile) -> Unit) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.itemLayout)
        private val iconText: TextView = view.findViewById(R.id.iconTextView)
        private val nameText: TextView = view.findViewById(R.id.nameTextView)
        private var currentTile: Tile? = null

        init {
            // Set onClickListener from attribute
            layout.setOnClickListener {
                currentTile?.let {
                    onClick(it)
                }
            }
        }

        fun bind(tile: Tile) {
            currentTile = tile

            // Set data
            nameText.text = tile.name
            var icon = tile.icon.replace("fa-", "")
            if (icon.startsWith("glyphicon-")) icon = icon.substring(icon.lastIndexOf("-") + 1)
            // replace some icons that we know don't work
            icon = icon.replace("mail-bulk", "comment-alt") // This does not even work on desktop
                    .replace("video-camera", "play") // This does work but looks horrible (Edupool)
                    .replace("project-diagram", "sitemap")

            iconText.text = icon

            // Set icon background color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (iconText.background as StateListDrawable).colorFilter = BlendModeColorFilter(tile.color, BlendMode.SRC_ATOP)
            } else {
                @Suppress("DEPRECATION") // not in < Q
                (iconText.background as StateListDrawable)
                        .setColorFilter(tile.color, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    // Creates new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_link, viewGroup, false)

        return ViewHolder(view, onClick)
    }

    // Replaces the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Bind data to ViewHolder
        viewHolder.bind(dataset[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataset.size

}