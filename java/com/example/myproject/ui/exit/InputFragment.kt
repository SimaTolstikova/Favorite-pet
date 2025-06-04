package com.example.myproject.ui.exit

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myproject.ApiClient
import com.example.myproject.ApiClient.apiService
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.databinding.FragmentInputBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InputFragment : Fragment() {

    private var _binding: FragmentInputBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InputViewModel by viewModels {
        InputViewModelFactory(ApiClient.apiService)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.llInput.setOnClickListener {
            binding.vInput.visibility = View.VISIBLE
            binding.vRegistr.visibility = View.GONE
            binding.llBtnInput.visibility = View.VISIBLE
            binding.llBtnRegistr.visibility = View.GONE
            binding.etPhone.setText("+7")
            binding.etPassword.text.clear()
        }

        binding.llRegistr.setOnClickListener {
            binding.vInput.visibility = View.GONE
            binding.vRegistr.visibility = View.VISIBLE
            binding.llBtnInput.visibility = View.GONE
            binding.llBtnRegistr.visibility = View.VISIBLE
            binding.etPhone.setText("+7")
            binding.etPassword.text.clear()
        }

        binding.etPhone.setOnClickListener {
            binding.etPhone.setText("+7")
        }

        binding.btnInput.setOnClickListener {
            val phone = binding.etPhone.text.toString()
            val password = binding.etPassword.text.toString()

            lifecycleScope.launch {
                val result = viewModel.login(phone, password)
                result.onSuccess {
                    val userIdResponse = viewModel.getUserId(phone)
                    if (userIdResponse.id_user != -1) {
                        val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putInt("id_user", userIdResponse.id_user)
                            putString("user_phone", phone) // Сохраняем номер телефона
                            apply()
                        }
                        Toast.makeText(requireContext(), "Вход успешен!", Toast.LENGTH_SHORT).show()
                        openMainScreen()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка: userId не найден", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    Toast.makeText(requireContext(), error.message ?: "Ошибка входа", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnRegistr.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Проверка формата телефона
            val phoneRegex = Regex("^(\\+7|8)[0-9]{10}$")
            if (!phone.matches(phoneRegex)) {
                binding.etPhone.error = "Введите номер в формате +7XXXXXXXXXX или 8XXXXXXXXXX"
                return@setOnClickListener
            }

            // Нормализация номера
            val normalizedPhone = if (phone.startsWith("8")) "+7${phone.substring(1)}" else phone

            // Проверка пароля
            if (password.length < 6) {
                binding.etPassword.error = "Пароль должен содержать минимум 6 символов"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val result = viewModel.register(normalizedPhone, password)
                    result.onSuccess { success ->
                        if (success) {
                            val userIdResponse = viewModel.getUserId(normalizedPhone)
                            if (userIdResponse.id_user != -1) {
                                requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .apply {
                                        putInt("id_user", userIdResponse.id_user)
                                        putString("user_phone", normalizedPhone) // Сохраняем номер телефона
                                        apply()
                                    }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                                    openMainScreen()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Ошибка: ID пользователя не получен", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }.onFailure { e ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, " ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("Registration", "Error: ${e.message}", e)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Registration", "Network error", e)
                    }
                }
            }
        }
    }

    private fun openMainScreen() {
        (requireActivity() as? MainActivity)?.showBottomNavigation()

        val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

        findNavController().navigate(R.id.exit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
