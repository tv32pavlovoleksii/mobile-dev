package com.pavlov.energymonitor.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pavlov.energymonitor.data.AppliancePreset
import com.pavlov.energymonitor.data.ApplianceRepository
import com.pavlov.energymonitor.domain.ApplianceInputValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** -1 означає «нового приладу ще немає id» — так само, як -1 у маршруті навігації. */
const val NEW_APPLIANCE_ID = -1

/**
 * ПЗ 6 (форма введення з валідацією) + ПЗ 7 (MVVM) + ПЗ 8 (збереження в Room).
 *
 * Поля — звичайний Compose State (як у ПЗ 4), бо вони належать лише цьому екрану
 * і не повинні «переживати» навігацію назад. А збереження — асинхронна операція
 * через [repository] із власним станом завантаження/помилки, типова для `ViewModel`.
 * Перевірка унікальності назви — приклад бізнес-логіки, яку не можна виконати
 * локально: вона звертається до бази даних, тому обов’язково `suspend`.
 */
class ApplianceFormViewModel(
    private val repository: ApplianceRepository,
    private val editingId: Int,
) : ViewModel() {
    val isEdit: Boolean get() = editingId != NEW_APPLIANCE_ID

    var name by mutableStateOf("")
    var power by mutableStateOf("")
    var hours by mutableStateOf("")
    var submitted by mutableStateOf(false)
    var saving by mutableStateOf(false)
    var saveError by mutableStateOf<String?>(null)
    var saved by mutableStateOf(false)
        private set

    val presets: List<AppliancePreset> = repository.presets()

    init {
        if (isEdit) {
            viewModelScope.launch {
                repository.find(editingId)?.let {
                    name = it.name
                    power = it.powerW.toString()
                    hours = ApplianceInputValidator.formatHours(it.hoursPerDay)
                }
            }
        }
    }

    val nameError: String? get() = if (submitted) ApplianceInputValidator.validateName(name) else null
    val powerError: String? get() = if (submitted) ApplianceInputValidator.parsePower(power).second else null
    val hoursError: String? get() = if (submitted) ApplianceInputValidator.parseHours(hours).second else null

    /** Підказка заповнює поля зразком — користувач може відразу змінити значення. */
    fun applyPreset(preset: AppliancePreset) {
        name = preset.name
        power = preset.powerW.toString()
        hours = ApplianceInputValidator.formatHours(preset.hoursPerDay)
    }

    fun submit(onSaved: () -> Unit) {
        submitted = true
        saveError = null
        val powerValue = ApplianceInputValidator.parsePower(power).first
        val hoursValue = ApplianceInputValidator.parseHours(hours).first
        if (ApplianceInputValidator.validateName(name) != null || powerValue == null || hoursValue == null) return

        viewModelScope.launch {
            saving = true
            if (repository.isNameTaken(name, editingId.takeIf { isEdit })) {
                saveError = "Прилад із такою назвою вже є у списку"
                saving = false
                return@launch
            }
            delay(500) // імітація збереження — щоб було видно індикатор завантаження
            if (isEdit) repository.update(editingId, name, powerValue, hoursValue)
            else repository.add(name, powerValue, hoursValue)
            saving = false
            saved = true
            onSaved()
        }
    }
}
