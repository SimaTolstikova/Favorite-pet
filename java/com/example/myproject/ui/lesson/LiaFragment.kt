package com.example.myproject.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myproject.databinding.FragmentLiaBinding

class LiaFragment : Fragment() {
    private var _binding: FragmentLiaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiaBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivArrow.setOnClickListener {
            val lessonFragment = requireParentFragment() as LessonFragment
            lessonFragment.showIcons()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}