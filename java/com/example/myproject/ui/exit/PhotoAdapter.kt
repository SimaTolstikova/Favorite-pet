package com.example.myproject.ui.exit

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myproject.databinding.ItemPhotoBinding

class PhotoAdapter(
    private val photoUris: List<Uri>,
    private val onItemClick: (Uri) -> Unit,
    private val onLongItemClick: (Uri) -> Boolean
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            binding.imageView.setImageURI(uri)

            binding.root.setOnClickListener {
                onItemClick(uri)
            }

            binding.root.setOnLongClickListener {
                onLongItemClick(uri)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoUris[position])
    }

    override fun getItemCount() = photoUris.size
}