package com.example.myproject.ui.exit

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myproject.ApiClient
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.databinding.FragmentExitBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class ExitFragment : Fragment() {

    private var _binding: FragmentExitBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InputViewModel by viewModels {
        InputViewModelFactory(ApiClient.apiService)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.formSubmitted = false

        // Загружаем и отображаем номер телефона пользователя
        loadUserPhone()

        binding.linearLayout.visibility = View.VISIBLE
        binding.llAll.visibility = View.VISIBLE
        binding.FragmentContainer.visibility = View.GONE

        binding.llPhoto.setOnClickListener {
            showFragment(PhotoFragment())
        }

        binding.llDoc.setOnClickListener {
            showFragment(DocFragment())
        }

        binding.llHistory.setOnClickListener {
            showFragment(HistoryFragment())
        }

        binding.ivExit.setOnClickListener {
            logoutUser()
        }
    }

    private fun loadUserPhone() {
        // Получаем номер телефона из SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val userPhone = sharedPref.getString("user_phone", getString(R.string.unknown_phone))

        // Устанавливаем номер в TextView
        binding.tvPhone.text = userPhone
    }

    private fun logoutUser() {
        val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("isLoggedIn", false)
            remove("id_user")  // Удаляем ID пользователя
            remove("user_phone") // Удаляем номер телефона
            apply()
        }

        (requireActivity() as? MainActivity)?.hideBottomNavigation()
        findNavController().navigate(R.id.input)
    }

    private fun showFragment(fragment: Fragment) {
        binding.linearLayout.visibility = View.GONE
        binding.llAll.visibility = View.GONE
        binding.FragmentContainer.visibility = View.VISIBLE

        childFragmentManager.commit {
            replace(R.id.FragmentContainer, fragment)
        }
    }

    fun restoreIcons() {
        childFragmentManager.findFragmentById(R.id.FragmentContainer)?.let { fragment ->
            childFragmentManager.commit {
                remove(fragment)
            }
        }
        binding.FragmentContainer.visibility = View.GONE
        binding.linearLayout.visibility = View.VISIBLE
        binding.llAll.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}