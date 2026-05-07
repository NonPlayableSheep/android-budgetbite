package de.fhe.budget_bite.view.search.recipe

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.fhe.budget_bite.storage.DatabaseRepository
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import javax.inject.Inject

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepository
) : ViewModel() {
    val recipesLiveData = databaseRepository.getAllRecipes()

    fun getRecipeById(mealItemId: Long): Recipe {
        return recipesLiveData.value!!.first { it.id == mealItemId }
    }
}