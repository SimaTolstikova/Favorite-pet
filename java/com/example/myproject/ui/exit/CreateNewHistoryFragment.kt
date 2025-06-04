package com.example.myproject.ui.exit

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myproject.ApiClient
import com.example.myproject.ApiClient.apiService
import com.example.myproject.History
import com.example.myproject.R
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateNewHistoryFragment : Fragment() {

    private lateinit var etHeading: EditText
    private lateinit var etText: EditText
    private lateinit var etDateStart: EditText
    private lateinit var etDateEnd: EditText
    private lateinit var sAnimal: Spinner
    private lateinit var btnSave: Button
    private lateinit var ivArrow: ImageView
    private lateinit var ivDelete: ImageView

    private val REQUEST_CODE_PERMISSION = 100

    private var history: History? = null
    private var animalIdList: List<Int> = emptyList() // список id животных для выбора

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_create_new_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etHeading = view.findViewById(R.id.etHeading)
        etText = view.findViewById(R.id.etText)
        etDateStart = view.findViewById(R.id.etDataStart)
        etDateEnd = view.findViewById(R.id.etDataEnd)
        sAnimal = view.findViewById(R.id.sAnimal)
        btnSave = view.findViewById(R.id.btnSave)
        ivArrow = view.findViewById(R.id.ivArrow)
        ivDelete = view.findViewById(R.id.ivDelete)

        loadAnimals()

        arguments?.getString("history")?.let { json ->
            history = Gson().fromJson(json, History::class.java)
            history?.let {
                etHeading.setText(it.heading)
                etText.setText(it.text)
                etDateStart.setText(it.dateStart)
                etDateEnd.setText(it.dateEnd)
                // установка значения спиннера произойдёт позже, после загрузки списка животных
            }
        }

        etDateStart.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                etDateStart.setText(formattedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        etDateEnd.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                etDateEnd.setText(formattedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSave.setOnClickListener {
            val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val id_user = sharedPref.getInt("id_user", -1)

            val heading = etHeading.text.toString().trim()
            val text = etText.text.toString().trim()
            val dateStart = etDateStart.text.toString().trim()
            val dateEnd = etDateEnd.text.toString().trim()
            val selectedAnimalIndex = sAnimal.selectedItemPosition

            if (heading.isBlank() || text.isBlank() || dateStart.isBlank() || dateEnd.isBlank() || selectedAnimalIndex < 0) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Проверка инициализации
            if (animalIdList.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Список животных не загружен. Подождите...",
                    Toast.LENGTH_SHORT).show()
                loadAnimals() // Перезагружаем данные
                return@setOnClickListener
            }

            // 2. Проверка выбора пользователя
            if (selectedAnimalIndex == -1) {
                Toast.makeText(requireContext(),
                    "Выберите животное из списка",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Безопасное получение ID
            val selectedAnimalId = try {
                animalIdList[selectedAnimalIndex]
            } catch (e: IndexOutOfBoundsException) {
                Log.e("CreateNewNapom", "Ошибка выбора животного: $e")
                Toast.makeText(requireContext(),
                    "Ошибка выбора животного",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val historyData = History(
                id_history = history?.id_history ?: 0,
                id_user = id_user,
                id_animal = selectedAnimalId,
                heading = heading,
                text = text,
                dateStart = convertToMysqlDate(dateStart),
                dateEnd = convertToMysqlDate(dateEnd)
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = if (history == null) {
                        Log.d("HistorySave", "Attempting to CREATE new history: $historyData")
                        apiService.insertHistory(historyData)
                    } else {
                        apiService.updateHistory(historyData)
                    }

                    val rawResponse = response.raw().toString()
                    Log.d("HistorySave", "Raw response: $rawResponse")

                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string()
                        Log.e("HistorySave",
                            "Error ${response.code()}: ${errorBody ?: "No error body"}")
                        throw IOException("API error: ${response.code()}")
                    }

                    withContext(Dispatchers.Main) {
                        showToast(if (history == null) "Успешно создано" else "Успешно обновлено")
                        findNavController().navigate(R.id.history)
                    }

                } catch (e: Exception) {
                    Log.e("HistorySave", "Full error:", e)
                    withContext(Dispatchers.Main) {
                        showToast("Ошибка: ${e.message ?: "unknown error"}")
                    }
                }
            }
        }

        ivDelete.setOnClickListener {
            history?.id_history?.let { historyId ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Создаем JSON объект с правильным именем поля
                        val requestBody = JsonObject().apply {
                            addProperty("id_history", historyId)
                        }
                        val response = ApiClient.apiService.deleteHistory(requestBody)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(requireContext(), "Удалено", Toast.LENGTH_SHORT).show()
                                findNavController().navigate(R.id.history)
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e("DeleteError", "Error: $errorBody")
                                Toast.makeText(requireContext(), "Ошибка удаления: $errorBody", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Log.e("DeleteError", "Exception: ${e.message}", e)
                            Toast.makeText(requireContext(), "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        ivArrow.setOnClickListener {
            findNavController().navigate(R.id.history)
        }
    }

    private fun convertToMysqlDate(input: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(input)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    private fun showToast(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAnimals() {
        val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val id_user = sharedPref.getInt("id_user", -1)

        if (id_user == -1) {
            showToast("Пользователь не авторизован")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("AnimalsLoad", "Запрашиваем животных для user $id_user")
                val response = ApiClient.apiService.getAnimalsByUserId(id_user)

                if (response.isSuccessful) {
                    val animals = response.body() ?: emptyList()
                    Log.d("AnimalsLoad", "Получено животных: ${animals.size}")

                    if (animals.isEmpty()) {
                        showToast("У вас нет зарегистрированных животных")
                        return@launch
                    }

                    val names = animals.map { it.nickname }
                    animalIdList = animals.map { it.id_animal }

                    withContext(Dispatchers.Main) {
                        ArrayAdapter(requireContext(),
                            android.R.layout.simple_spinner_item, names
                        ).apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            sAnimal.adapter = this
                            Log.d("AnimalsLoad", "Spinner обновлён")
                        }

                        // Автовыбор животного при редактировании
                        history?.id_animal?.let { animalId ->
                            val index = animalIdList.indexOf(animalId)
                            if (index >= 0) {
                                sAnimal.setSelection(index)
                                Log.d("AnimalsLoad", "Выбрано животное с индексом $index")
                            }
                        }
                    }
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("AnimalsLoad", "Ошибка API: $error")
                    showToast("Ошибка сервера: ${error ?: response.message()}")
                }
            } catch (e: Exception) {
                Log.e("AnimalsLoad", "Ошибка сети: ${e.message}", e)
                showToast("Ошибка сети: ${e.message}")
            }
        }
    }
}