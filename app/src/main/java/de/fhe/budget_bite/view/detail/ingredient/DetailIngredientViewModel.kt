package de.fhe.budget_bite.view.detail.ingredient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.fhe.budget_bite.storage.DatabaseRepository
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.utils.enums.MeasurementType
import de.fhe.budget_bite.utils.helper.NutritionCalculator.setNormalisedNutritionInfos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DetailIngredientViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepository
)  : ViewModel() {
    private var _ingredientLiveData: MutableLiveData<Ingredient> = MutableLiveData()
    private var _editIngredientLiveData: MutableLiveData<Ingredient> = MutableLiveData()
    private var _isIngredientChanged = false
    private var _isCreatingIngredient = false
    private var _isCreatingFirstLoad = true

    val ingredientLiveData: LiveData<Ingredient> get() = _ingredientLiveData
    val editIngredientLiveData: LiveData<Ingredient> get() = _editIngredientLiveData
    val isIngredientChanged get() = _isIngredientChanged
    val isCreatingIngredient get() = _isCreatingIngredient
    val isCreatingFirstLoad get() = _isCreatingFirstLoad

    fun createIngredient() {
        resetIsIngredientChanged()
        _isCreatingIngredient = true
        _isCreatingFirstLoad = true

        _ingredientLiveData.value = Ingredient()
        updateEditData()
    }

    fun loadIngredientById(ingredientId: Long) {
        resetIsIngredientChanged()
        resetIsCreatingIngredient()

        _ingredientLiveData = databaseRepository.getIngredientById(ingredientId) as MutableLiveData
    }

    private fun ingredientHasBeenChanged() {
        _isIngredientChanged = true
    }

    fun updateEditData() {
        _editIngredientLiveData.value = _ingredientLiveData.value!!.copy()
    }

    private fun resetIsIngredientChanged() {
        _isIngredientChanged = false
    }

    private fun resetIsCreatingIngredient() {
        _isCreatingIngredient = false
    }

    fun isCreatingFirstLoadDone() {
        _isCreatingFirstLoad = false
    }

    fun deleteIngredient() {
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.deleteIngredient(ingredientLiveData.value!!)
        }
    }

    fun updateIdentifier(identifier: String) {
        ingredientHasBeenChanged()
        _editIngredientLiveData.value!!.identifier = identifier
    }

    fun updateCalories(calories: Float) {
        ingredientHasBeenChanged()
        _editIngredientLiveData.value!!.embeddedNutritionInfo.calories = calories
    }

    fun updateCarbs(carbs: Float) {
        ingredientHasBeenChanged()
        _editIngredientLiveData.value!!.embeddedNutritionInfo.carbohydratesInGrams = carbs
    }

    fun updateFat(fat: Float) {
        ingredientHasBeenChanged()
        _editIngredientLiveData.value!!.embeddedNutritionInfo.fatInGrams = fat
    }

    fun updateProtein(protein: Float) {
        ingredientHasBeenChanged()
        _editIngredientLiveData.value!!.embeddedNutritionInfo.proteinInGrams = protein
    }

    fun updatePrice(price: Float) {
        ingredientHasBeenChanged()
        _editIngredientLiveData.value!!.embeddedNutritionInfo.price = price
    }

    fun updateMeasurementType(measurementType: MeasurementType) {
        ingredientHasBeenChanged()
        _editIngredientLiveData.value!!.measurementType = measurementType
    }

    fun updateMeasurementValue(measurementValue: Int) {
        ingredientHasBeenChanged()
        _editIngredientLiveData.value!!.measurementValueEntered = measurementValue
    }

    fun persistIngredient() {
        if (!isIngredientChanged) return

        viewModelScope.launch(Dispatchers.IO) {
            val ingredient = editIngredientLiveData.value!!
            setNormalisedNutritionInfos(ingredient)

            if (isCreatingIngredient) {
                val createdIngredient = ingredient.copy(
                    createdAt = LocalDateTime.now()
                )
                _ingredientLiveData = databaseRepository.insertIngredient(createdIngredient) as MutableLiveData

                resetIsCreatingIngredient()
            } else {
                databaseRepository.updateIngredient(ingredient)
            }

            resetIsIngredientChanged()
        }
    }
}