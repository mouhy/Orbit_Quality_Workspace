package com.orbit.mobile.feature.aireview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.EvaluateFileDto
import com.orbit.mobile.data.dto.EvaluateRequest
import com.orbit.mobile.data.dto.EvaluationResultDto
import com.orbit.mobile.data.dto.ReportTypeDto
import com.orbit.mobile.domain.repository.QualityRepository
import com.orbit.mobile.domain.repository.UploadPart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Upload limits
const val MAX_AI_FILES = 5
const val MAX_AI_IMAGES = 10
const val MAX_FILE_BYTES = 20L * 1024 * 1024
const val MAX_IMAGE_BYTES = 10L * 1024 * 1024

// Pass threshold
const val UI_PASS_SCORE = 70

// Submit state
data class SubmitState(
    val reportTypes: List<ReportTypeDto> = emptyList(),
    val selectedType: String? = null,
    val files: List<UploadPart> = emptyList(),
    val images: List<UploadPart> = emptyList(),
    val notes: String = "",
    val evaluating: Boolean = false,
    val result: EvaluationResultDto? = null,
    val error: UiText? = null
)

// Submit VM
@HiltViewModel
class TaskSubmitViewModel @Inject constructor(
    private val repo: QualityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubmitState())
    val state: StateFlow<SubmitState> = _state

    init {
        loadTypes()
    }

    // Load types
    fun loadTypes() {
        viewModelScope.launch {
            repo.reportTypes().onSuccess { types ->
                _state.update { it.copy(reportTypes = types) }
            }
        }
    }

    fun selectType(key: String?) = _state.update { it.copy(selectedType = key) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    // Add files
    fun addFiles(parts: List<UploadPart>) {
        _state.update { s ->
            val valid = parts.filter { it.bytes.size <= MAX_FILE_BYTES }
            s.copy(files = (s.files + valid).take(MAX_AI_FILES))
        }
    }

    fun removeFile(index: Int) {
        _state.update { s ->
            s.copy(files = s.files.filterIndexed { i, _ -> i != index })
        }
    }

    // Add images
    fun addImages(parts: List<UploadPart>) {
        _state.update { s ->
            val valid = parts.filter { it.bytes.size <= MAX_IMAGE_BYTES }
            s.copy(images = (s.images + valid).take(MAX_AI_IMAGES))
        }
    }

    fun removeImage(index: Int) {
        _state.update { s ->
            s.copy(images = s.images.filterIndexed { i, _ -> i != index })
        }
    }

    fun reset() {
        _state.update {
            SubmitState(reportTypes = it.reportTypes, selectedType = it.selectedType)
        }
    }

    // Run evaluation
    fun evaluate(taskTitle: String, taskDescription: String, taskId: String?) {
        val s = _state.value
        if (s.files.isEmpty() && s.images.isEmpty()) return
        _state.update { it.copy(evaluating = true, error = null, result = null) }
        viewModelScope.launch {
            val request = EvaluateRequest(
                taskTitle = taskTitle,
                taskDescription = taskDescription,
                taskId = taskId,
                reportType = s.selectedType,
                files = s.files.map { file ->
                    EvaluateFileDto(
                        fileName = file.fileName,
                        content = android.util.Base64.encodeToString(
                            file.bytes,
                            android.util.Base64.NO_WRAP
                        ),
                        fileType = file.mimeType
                    )
                },
                imageBase64 = s.images.map { image ->
                    android.util.Base64.encodeToString(
                        image.bytes,
                        android.util.Base64.NO_WRAP
                    )
                },
                submissionNotes = s.notes.ifBlank { null }
            )
            repo.evaluate(request)
                .onSuccess { result ->
                    _state.update { it.copy(evaluating = false, result = result) }
                }
                .onFailure { e ->
                    _state.update { it.copy(evaluating = false, error = e.toUiText()) }
                }
        }
    }
}
