package com.example.myproject.ui.exit

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myproject.R
import com.example.myproject.databinding.FragmentPhotoBinding

class PhotoFragment : Fragment() {

    private var _binding: FragmentPhotoBinding? = null
    private val binding get() = _binding!!

    private val photoUris = mutableListOf<Uri>()
    private lateinit var photoAdapter: PhotoAdapter

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            photoUris.add(it)
            photoAdapter.notifyItemInserted(photoUris.size - 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivArrow.setOnClickListener {
            (requireParentFragment() as ExitFragment).restoreIcons()
        }

        photoAdapter = PhotoAdapter(photoUris,
            onItemClick = { uri ->
                // Обработка обычного нажатия - просмотр фото
                val fullScreenFragment = FullScreenPhotoFragment().apply {
                    arguments = Bundle().apply {
                        putString("photoUri", uri.toString())
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.FragmentContainer, fullScreenFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onLongItemClick = { uri ->
                // Обработка долгого нажатия - удаление фото
                val position = photoUris.indexOf(uri)
                if (position != -1) {
                    photoUris.removeAt(position)
                    photoAdapter.notifyItemRemoved(position)
                }
                true // возвращаем true, чтобы показать, что событие обработано
            }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = photoAdapter

        binding.ivPlus.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}