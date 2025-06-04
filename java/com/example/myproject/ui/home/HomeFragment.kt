package com.example.myproject.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myproject.*
import com.example.myproject.ApiClient.apiService
import com.example.myproject.databinding.FragmentHomeBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArrayAdapter<String>
    private var animalList: List<Animal> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.lvAnimals.adapter = adapter

        loadAnimals()

        binding.lvAnimals.setOnItemClickListener { _, _, position, _ ->
            val animal = animalList[position]
            val bundle = Bundle().apply {
                putString("animal", Gson().toJson(animal))
            }
            findNavController().navigate(R.id.createNewAnimal, bundle)
        }

        binding.ivPlus.setOnClickListener {
            findNavController().navigate(R.id.createNewAnimal)
        }
    }

    private fun loadAnimals() {
        lifecycleScope.launch {
            try {
                val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val id_user = sharedPref.getInt("id_user", -1)

                Log.d("HomeFragment", "Loading animals for user ID: $id_user") // Логирование

                if (id_user == -1) {
                    Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val response = apiService.getAnimalsByUserId(id_user)
                if (response.isSuccessful) {
                    response.body()?.let { animals ->
                        Log.d("HomeFragment", "Received animals: ${animals.size}") // Логирование
                        animalList = animals
                        adapter.clear()
                        adapter.addAll(animals.map { it.nickname })
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(requireContext(), "Ошибка: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("HomeFragment", "Error loading animals", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
