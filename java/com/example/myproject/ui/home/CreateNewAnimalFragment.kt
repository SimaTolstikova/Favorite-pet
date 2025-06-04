package com.example.myproject.ui.home

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.myproject.ApiClient
import com.example.myproject.Animal
import com.example.myproject.R
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import retrofit2.Response

class CreateNewAnimalFragment : Fragment() {

    private lateinit var etName: EditText
    private lateinit var spinnerViewAnimal: Spinner
    private lateinit var etDate: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var etBreed: EditText
    private lateinit var btnSave: Button
    private lateinit var ivArrow: ImageView
    private lateinit var ivDelete: ImageView
    private lateinit var ibPhoto: ImageButton

    private val REQUEST_CODE_PERMISSION = 100
    private var isEditMode = false
    private var animalId: Int? = null
    private var selectedImageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_create_new_animal, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupImagePicker()
        setupArguments()
        setupDatePicker()
        setupButtons()
    }

    private fun initViews(view: View) {
        etName = view.findViewById(R.id.etName)
        spinnerViewAnimal = view.findViewById(R.id.spinnerViewAnimal)
        etDate = view.findViewById(R.id.etDate)
        spinnerGender = view.findViewById(R.id.spinnerGender)
        etBreed = view.findViewById(R.id.etBreed)
        btnSave = view.findViewById(R.id.btnSave)
        ivArrow = view.findViewById(R.id.ivArrow)
        ivDelete = view.findViewById(R.id.ivDelete)
        ibPhoto = view.findViewById(R.id.ibPhoto)
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    Glide.with(requireContext()).load(it).circleCrop().into(ibPhoto)
                }
            }
        }
    }

    private fun setupArguments() {
        arguments?.getString("animal")?.let { json ->
            val animal = Gson().fromJson(json, Animal::class.java)
            animal?.let {
                isEditMode = true
                animalId = it.id_animal
                populateFields(it)
            }
        }
        ivDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE
    }

    private fun populateFields(animal: Animal) {
        etName.setText(animal.nickname)
        etDate.setText(animal.date)
        etBreed.setText(animal.breed)
        spinnerViewAnimal.setSelection(animal.id_type)
        spinnerGender.setSelection(animal.id_gender)

        if (!animal.photo.isNullOrBlank()) {
            val photoUrl = "https://192.168.1.16/myproject/uploads/${animal.photo}"
            Glide.with(requireContext()).load(photoUrl).circleCrop().into(ibPhoto)
        }
    }

    private fun setupDatePicker() {
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                etDate.setText(formattedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupButtons() {
        btnSave.setOnClickListener { saveAnimal() }
        ivDelete.setOnClickListener { deleteAnimal() }
        ivArrow.setOnClickListener { findNavController().navigate(R.id.profile) }
        ibPhoto.setOnClickListener { checkPermissionAndPickImage() }
    }

    private fun saveAnimal() {
        val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val id_user = sharedPref.getInt("id_user", -1)

        val nickname = etName.text.toString().trim()
        val id_type = spinnerViewAnimal.selectedItemPosition
        val date = etDate.text.toString().trim()
        val id_gender = spinnerGender.selectedItemPosition
        val breed = etBreed.text.toString().trim()

        if (nickname.isBlank() || date.isBlank() || breed.isBlank()) {
            showToast("Заполните все поля")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Сначала загружаем изображение, если есть
                val photoUrl = selectedImageUri?.let { uploadImage(it) }

                // Создаем объект животного
                val animalData = Animal(
                    id_animal = animalId ?: 0,
                    id_user = id_user,
                    nickname = nickname,
                    id_type = id_type,
                    date = convertToMysqlDate(date),
                    id_gender = id_gender,
                    breed = breed,
                    photo = photoUrl ?: ""
                )

                // Отправляем данные на сервер
                val response = if (isEditMode) {
                    ApiClient.apiService.updateAnimal(animalData)
                } else {
                    ApiClient.apiService.insertAnimal(animalData)
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        showToast(if (isEditMode) "Данные обновлены" else "Животное добавлено")
                        findNavController().navigate(R.id.profile)
                    } else {
                        showToast("Ошибка сохранения: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Ошибка: ${e.localizedMessage}")
                    Log.e("SaveAnimal", "Error saving animal", e)
                }
            }
        }
    }

    private suspend fun uploadImage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempFile = File.createTempFile("upload", ".jpg", requireContext().cacheDir)
                inputStream.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val response = ApiClient.apiService.uploadImage(body)

                if (response.isSuccessful) {
                    response.body()?.string()
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UploadImage", "Error uploading image", e)
            null
        }
    }

    private fun deleteAnimal() {
        animalId?.let { id ->
            AlertDialog.Builder(requireContext())
                .setTitle("Удаление питомца")
                .setMessage("Вы уверены, что хотите удалить этого питомца?")
                .setPositiveButton("Удалить") { _, _ ->
                    performDelete(id)
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun performDelete(animalId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestBody = JsonObject().apply {
                    addProperty("id_animal", animalId)
                }

                val response = ApiClient.apiService.deleteAnimal(requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {  // Теперь isSuccessful будет доступен
                        showToast("Питомец удален")
                        findNavController().navigate(R.id.profile)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Неизвестная ошибка"
                        showToast("Ошибка удаления: $errorBody")
                        Log.e("DELETE_ERROR", "Code: ${response.code()}, Body: $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Ошибка сети: ${e.localizedMessage}")
                    Log.e("DELETE_ERROR", "Network error", e)
                }
            }
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        imagePickerLauncher.launch(intent)
    }

    private fun checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSION
            )
        } else {
            openImagePicker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSION && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openImagePicker()
        }
    }
}