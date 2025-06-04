package com.example.myproject.ui.napom

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myproject.ApiClient
import com.example.myproject.R
import com.example.myproject.Reminder
import com.example.myproject.ReminderScheduler
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CreateNewNapomFragment : Fragment() {

    private lateinit var etHeading: EditText
    private lateinit var etText: EditText
    private lateinit var etDate: EditText
    private lateinit var sTime: Spinner
    private lateinit var sAnimal: Spinner
    private lateinit var btnSave: Button
    private lateinit var ivArrow: ImageView
    private lateinit var ivDelete: ImageView

    private var reminder: Reminder? = null
    private var animalIdList: List<Int> = emptyList()
    private val timeSlots = (8..20).flatMap { hour ->
        (0..55 step 5).map { minute ->
            String.format("%02d:%02d", hour, minute)
        }
    }

    private val sharedPrefs: SharedPreferences by lazy {
        requireContext().getSharedPreferences("RemindersPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_create_new_napom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupTimeSpinner()
        loadAnimals()
        setupReminderData()
        setupListeners()
    }

    private fun initViews(view: View) {
        etHeading = view.findViewById(R.id.etHeading)
        etText = view.findViewById(R.id.etText)
        etDate = view.findViewById(R.id.etData)
        sTime = view.findViewById(R.id.sTime)
        sAnimal = view.findViewById(R.id.sAnimal)
        btnSave = view.findViewById(R.id.btnSave)
        ivArrow = view.findViewById(R.id.ivArrow)
        ivDelete = view.findViewById(R.id.ivDelete)
    }

    private fun setupTimeSpinner() {
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            timeSlots
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sTime.adapter = adapter
        }
    }

    private fun setupReminderData() {
        arguments?.getString("reminder")?.let { json ->
            reminder = Gson().fromJson(json, Reminder::class.java)
            reminder?.let {
                etHeading.setText(it.heading)
                etText.setText(it.text)
                etDate.setText(it.date)
                val timeIndex = timeSlots.indexOf(it.time)
                if (timeIndex >= 0) sTime.setSelection(timeIndex)
            }
        }
    }

    private fun setupListeners() {
        etDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { saveReminder() }
        ivDelete.setOnClickListener { deleteReminder() }
        ivArrow.setOnClickListener { findNavController().navigate(R.id.napom) }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveReminder() {
        val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val id_user = sharedPref.getInt("id_user", -1)

        val heading = etHeading.text.toString().trim()
        val text = etText.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = sTime.selectedItem.toString()
        val selectedAnimalIndex = sAnimal.selectedItemPosition

        if (heading.isBlank() || text.isBlank() || date.isBlank() || selectedAnimalIndex < 0) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedAnimalId = try {
            animalIdList[selectedAnimalIndex]
        } catch (e: IndexOutOfBoundsException) {
            Toast.makeText(requireContext(), "Ошибка выбора животного", Toast.LENGTH_SHORT).show()
            return
        }

        val reminderData = Reminder(
            id_reminder = reminder?.id_reminder ?: 0,
            id_user = id_user,
            id_animal = selectedAnimalId,
            heading = heading,
            text = text,
            date = date,
            time = time
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = if (reminder == null) {
                    ApiClient.apiService.insertReminder(reminderData)
                } else {
                    ApiClient.apiService.updateReminder(reminderData)
                }

                if (response.isSuccessful) {
                    saveReminderToPrefs(reminderData)
                    scheduleReminderNotifications(reminderData)
                    withContext(Dispatchers.Main) {
                        showToast(if (reminder == null) "Успешно создано" else "Успешно обновлено")
                        findNavController().navigate(R.id.napom)
                    }
                } else {
                    throw IOException("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Ошибка: ${e.message ?: "Неизвестная ошибка"}")
                }
            }
        }
    }

    private fun saveReminderToPrefs(reminder: Reminder) {
        val type = object : TypeToken<List<Reminder>>() {}.type
        val reminders = Gson().fromJson<List<Reminder>>(
            sharedPrefs.getString("reminders_list", "[]"), type
        ).toMutableList()

        reminders.removeAll { it.id_reminder == reminder.id_reminder }
        reminders.add(reminder)

        sharedPrefs.edit()
            .putString("reminders_list", Gson().toJson(reminders))
            .apply()
    }

    private fun scheduleReminderNotifications(reminder: Reminder) {
        ReminderScheduler(requireContext()).apply {
            if (reminder != null) cancelReminders(reminder.id_reminder)
            scheduleReminders(reminder)
        }
    }

    private fun deleteReminder() {
        reminder?.id_reminder?.let { id ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = ApiClient.apiService.deleteReminder(
                        JsonObject().apply { addProperty("id_reminder", id) }
                    )

                    if (response.isSuccessful) {
                        removeReminderFromPrefs(id)
                        ReminderScheduler(requireContext()).cancelReminders(id)
                        withContext(Dispatchers.Main) {
                            showToast("Удалено")
                            findNavController().navigate(R.id.napom)
                        }
                    } else {
                        throw IOException("Ошибка сервера: ${response.code()}")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("Ошибка удаления: ${e.message}")
                    }
                }
            }
        }
    }

    private fun removeReminderFromPrefs(reminderId: Int) {
        val type = object : TypeToken<List<Reminder>>() {}.type
        val reminders = Gson().fromJson<List<Reminder>>(
            sharedPrefs.getString("reminders_list", "[]"), type
        ).filter { it.id_reminder != reminderId }

        sharedPrefs.edit()
            .putString("reminders_list", Gson().toJson(reminders))
            .apply()
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
                val response = ApiClient.apiService.getAnimalsByUserId(id_user)

                if (response.isSuccessful) {
                    val animals = response.body() ?: emptyList()
                    animalIdList = animals.map { it.id_animal }

                    withContext(Dispatchers.Main) {
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            animals.map { it.nickname }
                        ).also { adapter ->
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            sAnimal.adapter = adapter
                        }

                        reminder?.id_animal?.let { id ->
                            val index = animalIdList.indexOf(id)
                            if (index >= 0) sAnimal.setSelection(index)
                        }
                    }
                } else {
                    showToast("Ошибка загрузки животных")
                }
            } catch (e: Exception) {
                showToast("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}