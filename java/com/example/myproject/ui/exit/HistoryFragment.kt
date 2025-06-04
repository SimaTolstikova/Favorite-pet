package com.example.myproject.ui.exit

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
import com.example.myproject.History
import com.example.myproject.R
import com.example.myproject.databinding.FragmentHistoryBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArrayAdapter<String>
    private var historyList: List<History> = emptyList()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.lvHistory.adapter = adapter

        loadHistorys()

        binding.lvHistory.setOnItemClickListener { _, _, position, _ ->
            val history = historyList[position]
            val bundle = Bundle().apply {
                putString("history", Gson().toJson(history))
            }
            findNavController().navigate(R.id.createNewHistory, bundle)
        }

        binding.ivPlus.setOnClickListener {
            findNavController().navigate(R.id.createNewHistory)
        }

        binding.ivArrow.setOnClickListener {
            findNavController().navigate(R.id.exit)
        }
    }

    private fun loadHistorys() {
        lifecycleScope.launch {
            try {
                val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val id_user = sharedPref.getInt("id_user", -1).also {
                    Log.d("HistoryFragment", "Loading historys for user ID: $it")
                }

                if (id_user == -1) {
                    showToast("Пользователь не авторизован")
                    return@launch
                }

                val response = apiService.getHistorysByUser(id_user)
                Log.d("HistoryFragment", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    when {
                        apiResponse == null -> {
                            Log.e("HistoryFragment", "Response body is null")
                            showToast("Ошибка: пустой ответ от сервера")
                        }
                        apiResponse.status != "success" -> {
                            Log.e("HistoryFragment", "API error status: ${apiResponse.status}")
                            showToast("Ошибка: ${apiResponse.status}")
                        }
                        else -> {
                            historyList = apiResponse.data
                            Log.d("HistoryFragment", "Loaded ${historyList.size} historys")

                            requireActivity().runOnUiThread {
                                adapter.clear()
                                adapter.addAll(historyList.map { it.heading })
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("HistoryFragment", "API error: $errorBody")
                    showToast("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Error loading historys", e)
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