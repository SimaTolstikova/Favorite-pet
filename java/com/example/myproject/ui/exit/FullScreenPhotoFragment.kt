package com.example.myproject.ui.exit

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.myproject.R
import com.example.myproject.databinding.FragmentFullScreenPhotoBinding

class FullScreenPhotoFragment : DialogFragment() {

    private var _binding: FragmentFullScreenPhotoBinding? = null
    private val binding get() = _binding!!


    //override fun onCreate(savedInstanceState: Bundle?) {
    //    super.onCreate(savedInstanceState)
    //    setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    //}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uri = arguments?.getString("photoUri")?.let { Uri.parse(it) }

        Glide.with(requireContext())
            .load(uri)
            .into(binding.fullScreenImage)

        binding.root.setOnClickListener {
            parentFragmentManager.popBackStack() // Закрытие по нажатию
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
