package de.fhe.budget_bite.view.search.add_meal_items

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemToAdd
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType

class AddMealItemsViewModel : ViewModel() {
    private var _isAddingMealItems = false
    private var addingToMealType: MealType = MealType.BREAKFAST
    private val _selectedMealItems = MutableLiveData<Set<MealItemToAdd>>(emptySet())
    private val selectedItemIds: MutableMap<MealItemType, MutableSet<Long>> =
        MealItemType.entries.associateWith { mutableSetOf<Long>() }.toMutableMap()
    private var previousSelectionSize = 0
    private var _hasAddedFirstItem = false

    val isAddingMealItems get() = _isAddingMealItems
    val selectedMealItems: LiveData<Set<MealItemToAdd>> get() = _selectedMealItems
    val hasAddedFirstItem get() = _hasAddedFirstItem

    fun toggleItemClicked(mealItemToAdd: MealItemToAdd) {
        _hasAddedFirstItem = false
        val currentSet = selectedMealItems.value ?: emptySet()

        val updatedSet = if (currentSet.contains(mealItemToAdd)) {
            currentSet - mealItemToAdd
        } else {
            currentSet + mealItemToAdd
        }

        toggleSelectedItemId(mealItemToAdd)

        val updatedSetSize = updatedSet.size
        if (previousSelectionSize == 0 && updatedSetSize == 1) {
            _hasAddedFirstItem = true
        }
        previousSelectionSize = updatedSetSize

        _selectedMealItems.value = updatedSet
    }

    private fun toggleSelectedItemId(mealItemToAdd: MealItemToAdd) {
        val mealItemId = mealItemToAdd.mealItemId
        val mealItemTypeIds = selectedItemIds[mealItemToAdd.mealItemType]!!

        if (mealItemTypeIds.contains(mealItemId)) {
            mealItemTypeIds.remove(mealItemId)
        } else {
            mealItemTypeIds.add(mealItemId)
        }
    }

    fun createMealItemToAdd(mealItemId: Long, ingredient: Ingredient? = null, recipe: Recipe? = null): MealItemToAdd {
        val mealItemType = if (ingredient != null) {
            MealItemType.INGREDIENT
        } else {
            MealItemType.RECIPE
        }
        return MealItemToAdd(
            mealItemId,
            mealItemType,
            ingredient,
            recipe
        )
    }

    fun isMealItemSelected(mealItemId: Long, mealItemType: MealItemType): Boolean {
        return selectedItemIds[mealItemType]!!.contains(mealItemId)
    }

    fun getSelectedMealItemsToAdd() = selectedMealItems.value!!.toList()

    fun getAddingMealType() = addingToMealType

    fun resetAdding() {
        _isAddingMealItems = false
        _selectedMealItems.value = emptySet()
        previousSelectionSize = 0
        for (mealItemType in selectedItemIds.keys) {
            selectedItemIds[mealItemType]!!.clear()
        }
    }

    // from DetailViewModel
    fun isAddingMealItems(toMealType: MealType) {
        addingToMealType = toMealType
        _isAddingMealItems = true
    }
}