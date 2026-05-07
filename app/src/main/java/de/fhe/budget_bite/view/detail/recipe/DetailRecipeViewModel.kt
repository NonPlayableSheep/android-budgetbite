package de.fhe.budget_bite.view.detail.recipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.fhe.budget_bite.storage.DatabaseRepository
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.storage.entity.recipe.RecipeIngredient
import de.fhe.budget_bite.storage.entity.recipe.RecipeWithRecipeIngredients
import de.fhe.budget_bite.utils.enums.ChangeStatus
import de.fhe.budget_bite.utils.helper.NutritionCalculator.applyItemNutritionToTotal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DetailRecipeViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepository
) : ViewModel() {
    private var _recipeWithIngredientsLiveData: MutableLiveData<RecipeWithRecipeIngredients> = MutableLiveData()
    private var _editRecipeWithIngredientsLiveData: MutableLiveData<RecipeWithRecipeIngredients> = MutableLiveData()
    private var _editRecipeNutritionInfo: MutableLiveData<EmbeddedNutritionInfo> = MutableLiveData()
    private var _isAddingIngredients = false
    private var _isRecipeChanged = false
    private var _isCreatingRecipe = false

    val recipeWithIngredientsLiveData: LiveData<RecipeWithRecipeIngredients> get() = _recipeWithIngredientsLiveData
    val editRecipeWithRecipeIngredientsLiveData: LiveData<RecipeWithRecipeIngredients> get() = _editRecipeWithIngredientsLiveData
    val editRecipeNutritionInfo: LiveData<EmbeddedNutritionInfo> get() = _editRecipeNutritionInfo
    val isAddingIngredients get() = _isAddingIngredients
    val isRecipeChanged get() = _isRecipeChanged
    val isCreatingRecipe get() = _isCreatingRecipe

    fun createRecipe() {
        resetIsRecipeChanged()
        _isCreatingRecipe = true

        _recipeWithIngredientsLiveData.value = RecipeWithRecipeIngredients()
        updateEditData()
    }

    fun loadRecipeById(recipeId: Long) {
        resetIsRecipeChanged()
        resetIsCreatingRecipe()

        _recipeWithIngredientsLiveData = databaseRepository.getRecipeWithIngredients(recipeId) as MutableLiveData
    }

    fun updateEditData() {
        _editRecipeWithIngredientsLiveData.value = _recipeWithIngredientsLiveData.value!!.copy()
        _editRecipeNutritionInfo.value = _recipeWithIngredientsLiveData.value!!.recipe.embeddedNutritionInfoInTotal.copy()
    }

    private fun recipeHasBeenChanged() {
        _isRecipeChanged = true
    }

    fun isAddingIngredients() {
        _isAddingIngredients = true
    }

    // only used during loading/ creating
    private fun resetIsRecipeChanged() {
        _isRecipeChanged = false
    }

    private fun resetIsCreatingRecipe() {
        _isCreatingRecipe = false
    }

    fun getIngredientIndexFromActiveIngredients(ingredientId: Long): Int {
        val activeIngredients = editRecipeWithRecipeIngredientsLiveData.value!!.ingredients
            .filter { it.changeStatus != ChangeStatus.DELETED }

        return activeIngredients.indexOfFirst { it.ingredient.id == ingredientId }
    }

    fun getIdsOfActiveIngredients(): List<Long> {
        return editRecipeWithRecipeIngredientsLiveData.value!!.ingredients
            .filter { it.changeStatus != ChangeStatus.DELETED }
            .map { it.ingredient.id }
    }

    fun setRecipeIdentifier(identifier: String) {
        recipeHasBeenChanged()
        _editRecipeWithIngredientsLiveData.value!!.recipe.identifier = identifier
    }

    fun deleteRecipeWithIngredients() {
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.deleteRecipeWithIngredients(recipeWithIngredientsLiveData.value!!)
        }
    }

    fun persistRecipeWithIngredients() {
        if (!isRecipeChanged) return

        viewModelScope.launch(Dispatchers.IO) {
            val recipeNutritionInfo = editRecipeNutritionInfo.value!!
            val baseRecipe = editRecipeWithRecipeIngredientsLiveData.value!!.recipe.copy(
                embeddedNutritionInfoInTotal = recipeNutritionInfo
            )

            val recipe = if (isCreatingRecipe) {
                baseRecipe.copy(createdAt = LocalDateTime.now())
            } else {
                baseRecipe
            }

            val recipeWithIngredients = editRecipeWithRecipeIngredientsLiveData.value!!.copy(
                recipe = recipe
            )

            if (isCreatingRecipe) {

                _recipeWithIngredientsLiveData = databaseRepository.insertRecipeWithIngredients(recipeWithIngredients) as MutableLiveData
                resetIsCreatingRecipe()
            } else {
                databaseRepository.updateRecipeWithIngredients(recipeWithIngredients)
            }

            resetIsRecipeChanged()
        }
    }

    fun resetIsAddingIngredients() {
        _isAddingIngredients = false
    }

    fun addIngredientsToRecipe(ingredients: List<Ingredient>) {
        recipeHasBeenChanged()
        resetIsAddingIngredients()

        // Hole die aktuelle Liste von Zutaten und filtere sie nach Status
        val recipeIngredients = _editRecipeWithIngredientsLiveData.value!!.ingredients

        // Filtere aktive und gelöschte Zutaten
        val activeIngredients = recipeIngredients.filter { it.changeStatus != ChangeStatus.DELETED }.toMutableList()
        val deletedIngredients = recipeIngredients.filter { it.changeStatus == ChangeStatus.DELETED }

        // Bestimme die aktuelle Anzahl aktiver Zutaten
        val currentRecipeIngredientsCount = activeIngredients.size

        // Füge die neuen Zutaten hinzu
        for ((index, ingredient) in ingredients.withIndex()) {
            activeIngredients.add(
                RecipeIngredient(
                    ingredient,
                    0,
                    index + currentRecipeIngredientsCount,
                    ChangeStatus.NEW
                )
            )
        }

        // Kombiniere die aktiven und gelöschten Zutaten
        val updatedIngredients = activeIngredients + deletedIngredients

        // Aktualisiere die LiveData
        _editRecipeWithIngredientsLiveData.value = editRecipeWithRecipeIngredientsLiveData.value!!.copy(ingredients = updatedIngredients)
    }

    fun deleteIngredientFromRecipeById(ingredientId: Long) {
        recipeHasBeenChanged()
        val ingredients = _editRecipeWithIngredientsLiveData.value!!.ingredients
        val sortedIngredients = ingredients.toMutableList()
        val deletedIngredient = sortedIngredients.first { it.ingredient.id == ingredientId }
        val deletedIngredientIndex = sortedIngredients.indexOf(deletedIngredient)

        deletedIngredient.apply {
            this.changeStatus = ChangeStatus.DELETED
            val ingredientQuantity = this.quantity
            val ingredientInfos = this.ingredient.normalisedEmbeddedNutritionInfo

            updateNutritionInfoLiveData(-ingredientQuantity, ingredientInfos)
        }

        sortedIngredients.forEachIndexed { index, recipeIngredient ->
            if (index > deletedIngredientIndex && recipeIngredient.changeStatus != ChangeStatus.DELETED) {
                recipeIngredient.apply {
                    sequenceIndex -= 1
                    if (changeStatus == ChangeStatus.UNCHANGED) {
                        changeStatus = ChangeStatus.CHANGED
                    }
                }
            }
        }

        _editRecipeWithIngredientsLiveData.value!!.ingredients = sortedIngredients
    }

    private fun updateNutritionInfoLiveData(quantity: Int, ingredientInfo: EmbeddedNutritionInfo) {
        applyItemNutritionToTotal(
            _editRecipeNutritionInfo.value!!,
            ingredientInfo,
            quantity
        )
        _editRecipeNutritionInfo.value = _editRecipeNutritionInfo.value
    }

    fun updateIngredientQuantityInRecipe(ingredientId: Long, quantityUpdate: Int) {
        recipeHasBeenChanged()
        _editRecipeWithIngredientsLiveData.value!!.ingredients.first { it.ingredient.id == ingredientId }
            .apply {
                if (changeStatus == ChangeStatus.UNCHANGED) {
                    changeStatus = ChangeStatus.CHANGED
                }
                val ingredientInfo: EmbeddedNutritionInfo = this.ingredient.normalisedEmbeddedNutritionInfo
                val quantityDifference: Int = quantityUpdate - this.quantity
                this.quantity = quantityUpdate

                updateNutritionInfoLiveData(quantityDifference, ingredientInfo)
            }
    }
}