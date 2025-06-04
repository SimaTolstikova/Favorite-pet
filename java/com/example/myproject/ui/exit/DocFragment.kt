package com.example.myproject.ui.exit

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myproject.ApiClient
import com.example.myproject.Document
import com.example.myproject.R
import com.example.myproject.databinding.FragmentDocBinding
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class DocFragment : Fragment() {

    private var _binding: FragmentDocBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ArrayAdapter<String>
    private var documentList = mutableListOf<Document>()
    private var currentUserId: Int = -1

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { uploadDocument(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupClickListeners()
        loadUserData()
        loadUserDocuments()
    }

    private fun setupAdapter() {
        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_2,
            android.R.id.text1,
            mutableListOf()
        )
        binding.lvDocument.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.lvDocument.setOnItemClickListener { _, _, position, _ ->
            documentList.getOrNull(position)?.let { doc ->
                openDocument(doc.file)
            }
        }

        binding.lvDocument.setOnItemLongClickListener { _, _, position, _ ->
            documentList.getOrNull(position)?.let { doc ->
                showDeleteDialog(doc.id_document)
            }
            true
        }

        binding.ivPlus.setOnClickListener {
            if (currentUserId != -1) openFilePicker()
            else showToast("Требуется авторизация")
        }

        binding.ivArrow.setOnClickListener {
            findNavController().navigate(R.id.exit)
        }
    }

    private fun loadUserData() {
        currentUserId = requireContext()
            .getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getInt("id_user", -1)
    }

    private fun loadUserDocuments() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                if (currentUserId == -1) {
                    showToast("Пользователь не авторизован")
                    return@launch
                }

                val response = ApiClient.apiService.getDocumentByUser(currentUserId)

                if (response.isSuccessful) {
                    response.body()?.let { documents ->
                        documentList.clear()
                        documentList.addAll(documents)
                        updateAdapter()
                    } ?: showToast("Документы не найдены")
                } else {
                    handleErrorResponse(response.code())
                }
            } catch (e: Exception) {
                showToast("Ошибка: ${e.localizedMessage}")
                Log.e("DOC_ERROR", "Load error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateAdapter() {
        adapter.clear()
        adapter.addAll(documentList.map { "${it.text}\n${it.file}" })
    }

    private fun handleErrorResponse(code: Int) {
        when (code) {
            404 -> showToast("Документы не найдены")
            401 -> showToast("Требуется авторизация")
            else -> showToast("Ошибка сервера: $code")
        }
    }

    private fun showDeleteDialog(documentId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление документа")
            .setMessage("Вы уверены, что хотите удалить этот документ?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteDocument(documentId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteDocument(documentId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestBody = JsonObject().apply {
                    addProperty("id_document", documentId)
                }

                val response = ApiClient.apiService.deleteDocument(requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        documentList.removeAll { it.id_document == documentId }
                        updateAdapter()
                        showToast("Документ удалён")
                    } else {
                        val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                        showToast("Ошибка удаления: $error")
                        Log.e("DELETE_ERROR", "Code: ${response.code()}, Error: $error")
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

    private fun uploadDocument(uri: Uri) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val fileName = getFileName(uri) ?: "document_${System.currentTimeMillis()}"
                val file = File(requireContext().cacheDir, fileName).apply {
                    createNewFile()
                }

                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                if (file.length() > 10 * 1024 * 1024) {
                    showToast("Файл слишком большой (макс. 10MB)")
                    return@launch
                }

                val requestFile = file.asRequestBody(
                    requireContext().contentResolver.getType(uri)?.toMediaTypeOrNull()
                        ?: "multipart/form-data".toMediaTypeOrNull()
                )
                val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)

                val response = ApiClient.apiService.insertDocument(
                    userId = currentUserId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = "Новый документ".toRequestBody("text/plain".toMediaTypeOrNull()),
                    file = filePart
                )

                if (response.isSuccessful) {
                    showToast("Файл успешно загружен")
                    loadUserDocuments()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    showToast("Ошибка загрузки: $error")
                    Log.e("UPLOAD_ERROR", "Upload failed: $error")
                }
            } catch (e: Exception) {
                showToast("Ошибка: ${e.localizedMessage}")
                Log.e("UPLOAD_ERROR", "Upload failed", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openDocument(filename: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                if (filename.isBlank()) {
                    showToast("Неверное имя файла")
                    return@launch
                }

                val localFile = File(requireContext().cacheDir, filename)

                // Логирование для отладки
                Log.d("DOC_DEBUG", "Opening document: $filename")
                Log.d("DOC_DEBUG", "Local path: ${localFile.absolutePath}")

                if (localFile.exists() && localFile.length() > 0) {
                    openLocalFile(localFile)
                    return@launch
                }

                // Формируем URL для скачивания
                val baseUrl = if (ApiClient.BASE_URL.endsWith("/"))
                    ApiClient.BASE_URL
                else
                    "${ApiClient.BASE_URL}/"

                val fileUrl = "${baseUrl}documents/$filename"
                Log.d("DOC_DEBUG", "Trying to download from: $fileUrl")

                try {
                    val response = ApiClient.apiService.downloadDocument(filename)

                    if (response.isSuccessful) {
                        response.body()?.byteStream()?.use { inputStream ->
                            FileOutputStream(localFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        openLocalFile(localFile)
                    } else {
                        tryOpenInBrowser(fileUrl)
                    }
                } catch (e: Exception) {
                    Log.e("DOC_DOWNLOAD", "Download failed", e)
                    tryOpenInBrowser(fileUrl)
                }
            } catch (e: Exception) {
                showToast("Ошибка открытия документа")
                Log.e("DOC_OPEN", "Error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openLocalFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val mimeType = getMimeType(file.name) ?: "application/octet-stream"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showToast("Нет приложения для открытия этого файла")
        } catch (e: Exception) {
            showToast("Ошибка открытия файла")
            Log.e("OPEN_FILE", "Error opening file", e)
        }
    }

    private fun tryOpenInBrowser(url: String) {
        try {
            if (!URLUtil.isValidUrl(url)) {
                showToast("Некорректная ссылка")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Невозможно открыть ссылку")
            Log.e("BROWSER_OPEN", "Error", e)
        }
    }

    private fun openFilePicker() {
        openDocumentLauncher.launch(arrayOf("*/*"))
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    private fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
