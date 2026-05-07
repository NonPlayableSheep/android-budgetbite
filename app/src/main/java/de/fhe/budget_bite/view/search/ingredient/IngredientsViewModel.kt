package de.fhe.budget_bite.view.search.ingredient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.fhe.budget_bite.storage.DatabaseRepository
import de.fhe.budget_bite.storage.entity.Ingredient
import javax.inject.Inject

@HiltViewModel
class IngredientsViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepository
) : ViewModel() {
    private var _ingredientsLiveData = databaseRepository.getAllIngredients()
    private val _clickedIngredientIds = MutableLiveData<Set<Long>>(emptySet())
    private var previousSelectionSize = 0
    private var _isFirstIngredientAdded = false

    val ingredientsLiveData: LiveData<List<Ingredient>> get() = _ingredientsLiveData
    val clickedIngredientIds: LiveData<Set<Long>> get() = _clickedIngredientIds
    val isFirstIngredientAdded get() = _isFirstIngredientAdded

    fun getIngredientsNotInRecipe(ingredientInRecipeIds: List<Long>) {
        _ingredientsLiveData = databaseRepository.getAllIngredientsNotInParam(ingredientInRecipeIds)
    }

    fun getAllIngredients() {
        _ingredientsLiveData = databaseRepository.getAllIngredients()
    }

    fun toggleIngredientClicked(ingredientId: Long) {
        _isFirstIngredientAdded = false

        val currentSet = _clickedIngredientIds.value ?: emptySet()

        val updatedSet = if (currentSet.contains(ingredientId)) {
            currentSet - ingredientId
        } else {
            currentSet + ingredientId
        }

        val newSelectionSize = updatedSet.size
        if (previousSelectionSize == 0 && newSelectionSize == 1) {
            _isFirstIngredientAdded = true
        }

        previousSelectionSize = newSelectionSize

        _clickedIngredientIds.value = updatedSet
    }

    fun isIngredientClicked(ingredientId: Long): Boolean {
        return _clickedIngredientIds.value?.contains(ingredientId) == true
    }

    fun getClickedIngredients(): List<Ingredient> {
        val ingredients = ingredientsLiveData.value!!
        val clickedIds = _clickedIngredientIds.value!!

        return ingredients.filter { ingredient -> ingredient.id in clickedIds }
    }

    fun clearClickedIngredients() {
        previousSelectionSize = 0
        _clickedIngredientIds.value = emptySet()
    }

    fun getIngredientById(mealItemId: Long): Ingredient {
        return ingredientsLiveData.value!!.first { it.id == mealItemId }
    }
}