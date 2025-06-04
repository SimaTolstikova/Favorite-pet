package com.example.myproject.ui.napom

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myproject.ApiClient.apiService
import com.example.myproject.R
import com.example.myproject.Reminder
import com.example.myproject.databinding.FragmentNapomBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class NapomFragment : Fragment() {

    private var _binding: FragmentNapomBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArrayAdapter<String>
    private var reminderList: List<Reminder> = emptyList()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNapomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.lvNapom.adapter = adapter

        loadReminders()

        binding.lvNapom.setOnItemClickListener { _, _, position, _ ->
            val reminder = reminderList[position]
            val bundle = Bundle().apply {
                putString("reminder", Gson().toJson(reminder))
            }
            findNavController().navigate(R.id.createNewNapom, bundle)
        }

        binding.ivPlus.setOnClickListener {
            findNavController().navigate(R.id.createNewNapom)
        }
    }

    private fun loadReminders() {
        lifecycleScope.launch {
            try {
                val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val id_user = sharedPref.getInt("id_user", -1).also {
                    Log.d("NapomFragment", "Loading reminders for user ID: $it")
                }

                if (id_user == -1) {
                    showToast("Пользователь не авторизован")
                    return@launch
                }

                val response = apiService.getRemindersByUser(id_user)
                Log.d("NapomFragment", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    when {
                        apiResponse == null -> {
                            Log.e("NapomFragment", "Response body is null")
                            showToast("Ошибка: пустой ответ от сервера")
                        }
                        apiResponse.status != "success" -> {
                            Log.e("NapomFragment", "API error status: ${apiResponse.status}")
                            showToast("Ошибка: ${apiResponse.status}")
                        }
                        else -> {
                            reminderList = apiResponse.data
                            Log.d("NapomFragment", "Loaded ${reminderList.size} reminders")

                            requireActivity().runOnUiThread {
                                adapter.clear()
                                adapter.addAll(reminderList.map { it.heading })
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("NapomFragment", "API error: $errorBody")
                    showToast("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NapomFragment", "Error loading reminders", e)
                showToast("Ошибка загрузки: ${e.message}")
            }
        }
    }

    private fun showToast(message: String?) {
        requireActivity().runOnUiThread {
            Toast.makeText(
                requireContext(),
                message ?: "Неизвестная ошибка",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}