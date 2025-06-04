package com.example.myproject.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.myproject.databinding.FragmentMapBinding

data class Location(val city: String, val address: String, val category: String)

class Map : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()

    private lateinit var allLocations: List<Location>
    private lateinit var listViewAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Данные (замените на свои)
        allLocations = listOf(
            Location("Ярославль", "2-я Бутырская улица", "Ветеринарная клиника"),
            Location("Ярославль", "район Всполье", "Ветеринарная клиника"),
            Location("Ярославль", "улица Юности, 23/79", "Ветеринарная клиника"),
            Location("Ростов-на-Дону", "Улица Пушкина, 10", "Ветеринарная клиника"),

            Location("Ярославль", "Крохинский лесопарк", "Место для выгула"),
            Location("Ярославль", "жилой район Пятёрка", "Место для выгула"),
            Location("Рыбинск", "проспект Ленина, 181", "Место для выгула"),

            Location("Ярославль", "Автозаводская улица, 95", "Зоомагазин"),
            Location("Ярославль", "3-я Новодуховская улица", "Зоомагазин"),
            Location("Ростов-на-Дону", "Большая Садовая, 5", "Зоомагазин")
        )

        // Настройка Spinner для выбора города
        val cities = allLocations.map { it.city }.distinct().toMutableList()
        cities.add(0, "Все города")
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sSity.adapter = cityAdapter

        // Настройка Spinner для выбора категории заведений
        val categories = listOf("Все заведения", "Место для выгула", "Ветеринарная клиника", "Зоомагазин")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sCategory.adapter = categoryAdapter

        // Установка адаптера для списка адресов (первый запуск с полным списком)
        updateAddressList("Все города", "Все заведения")

        // Обработчик выбора города
        binding.sSity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCity = cities[position]
                val selectedCategory = binding.sCategory.selectedItem.toString()
                updateAddressList(selectedCity, selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Обработчик выбора категории заведения
        binding.sCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCity = binding.sSity.selectedItem.toString()
                val selectedCategory = categories[position]
                updateAddressList(selectedCity, selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateAddressList(selectedCity: String, selectedCategory: String) {
        val filteredAddresses = allLocations.filter {
            (selectedCity == "Все города" || it.city == selectedCity) &&
                    (selectedCategory == "Все заведения" || it.category == selectedCategory)
        }.map {
            if (selectedCategory == "Все заведения") {
                "${it.category} \n ${it.city}, ${it.address}"
            } else {
                "${it.address}"
            }
        }

        listViewAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, filteredAddresses)
        binding.lvList.adapter = listViewAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
