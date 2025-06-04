package com.example.myproject.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.example.myproject.R
import com.example.myproject.databinding.FragmentLessonBinding

class LessonFragment : Fragment() {

    private lateinit var llSit: LinearLayout
    private lateinit var llStand: LinearLayout
    private lateinit var llLie: LinearLayout

    private var _binding: FragmentLessonBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(LessonViewModel::class.java)

        _binding = FragmentLessonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llSit = binding.llSit
        llStand = binding.llStand
        llLie = binding.llLie

        llSit.setOnClickListener {
            showFragment(SitFragment())
            binding.llCom.visibility = View.GONE
            binding.fragmentContainerView.visibility = View.VISIBLE
            binding.textHome.visibility = View.GONE
        }
        llStand.setOnClickListener {
            showFragment(StandFragment())
            binding.llCom.visibility = View.GONE
            binding.fragmentContainerView.visibility = View.VISIBLE
            binding.textHome.visibility = View.GONE
        }
        llLie.setOnClickListener {
            showFragment(LiaFragment())
            binding.llCom.visibility = View.GONE
            binding.fragmentContainerView.visibility = View.VISIBLE
            binding.textHome.visibility = View.GONE
        }
    }

    private fun showFragment(fragment: Fragment) {
        childFragmentManager.commit {
            replace(R.id.fragmentContainerView, fragment)
        }
    }

    fun showIcons() {
        childFragmentManager.commit {
            replace(R.id.fragmentContainerView,  Fragment())
        }
        binding.llCom.visibility = View.VISIBLE
        binding.fragmentContainerView.visibility = View.GONE
        binding.textHome.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}