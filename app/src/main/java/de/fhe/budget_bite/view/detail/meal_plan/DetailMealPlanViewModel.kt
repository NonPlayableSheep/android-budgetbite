package de.fhe.budget_bite.view.detail.meal_plan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.fhe.budget_bite.storage.DatabaseRepository
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemToAdd
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemWithDetails
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlanWithItems
import de.fhe.budget_bite.utils.enums.ChangeStatus
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType
import de.fhe.budget_bite.utils.helper.NutritionCalculator.applyItemNutritionToTotal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DetailMealPlanViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepository
) : ViewModel() {
    private var _mealPlanLiveData: MutableLiveData<MealPlanWithItems> = MutableLiveData()
    private var _editMealPlanLiveData: MutableLiveData<MealPlanWithItems> = MutableLiveData()
    private var _editNutritionInfo: MutableLiveData<EmbeddedNutritionInfo> = MutableLiveData()
    private var _isMealPlanChanged = false
    private var _isCreatingMealPlan = false
    private var updatedMealType: MealType? = null
    private var updatedMealItemId: Long? = null
    private var nextNegativeId = 0L

    val mealPlanLiveData: LiveData<MealPlanWithItems> get() = _mealPlanLiveData
    val editMealPlanLiveData: LiveData<MealPlanWithItems> get() = _editMealPlanLiveData
    val editNutritionInfo: LiveData<EmbeddedNutritionInfo> get() = _editNutritionInfo
    val isMealPlanChanged get() = _isMealPlanChanged
    val isCreatingMealPlan get() = _isCreatingMealPlan

    private fun getNextNegativeId(): Long {
        nextNegativeId--
        return nextNegativeId
    }

    private fun resetNextNegativeId() {
        nextNegativeId = 0L
    }

    private fun markMealTypeUpdated(mealType: MealType) {
        updatedMealType = mealType
        updatedMealItemId = null
    }

    private fun markMealItemUpdated(mealType: MealType, mealItemId: Long) {
        updatedMealType = mealType
        updatedMealItemId = mealItemId
    }

    fun getUpdatedMealType(): MealType? = updatedMealType
    fun getUpdatedMealItemId(): Long? = updatedMealItemId
    fun resetUpdateTracking() {
        updatedMealType = null
        updatedMealItemId = null
    }

    fun getUpdatedMealItemQuantity(): Int {
        return editMealPlanLiveData.value!!.itemsByMealType[updatedMealType]!!
            .first { it.id == updatedMealItemId }.quantity
    }

    fun getUpdatedMealItemNutritionInfo(): EmbeddedNutritionInfo {
        val updatedMealItem = editMealPlanLiveData.value!!.itemsByMealType[updatedMealType]!!
            .first { it.id == updatedMealItemId }
        return when (updatedMealItem.mealItemType) {
            MealItemType.INGREDIENT -> updatedMealItem.ingredient!!.normalisedEmbeddedNutritionInfo
            MealItemType.RECIPE -> updatedMealItem.recipe!!.embeddedNutritionInfoInTotal
        }
    }

    fun updateEditData() {
        _editMealPlanLiveData.value = mealPlanLiveData.value!!.copy()
        _editNutritionInfo.value = mealPlanLiveData.value!!.mealPlan.embeddedNutritionInfoInTotal.copy()
    }

    fun loadMealPlanById(mealPlanId: Long) {
        resetIsMealPlanChanged()
        resetNextNegativeId()

        _mealPlanLiveData = databaseRepository.getMealPlanWithItems(mealPlanId) as MutableLiveData
    }

    fun createMealPlan() {
        resetIsMealPlanChanged()
        resetNextNegativeId()
        _isCreatingMealPlan = true

        _mealPlanLiveData.value = MealPlanWithItems()
        updateEditData()
    }

    private fun mealPlanHasBeenChanged() {
        _isMealPlanChanged = true
    }

    private fun resetIsMealPlanChanged() {
        _isMealPlanChanged = false
    }

    private fun resetIsCreatingMealPlan() {
        _isCreatingMealPlan = false
    }

    fun setMealPlanIdentifier(identifier: String) {
        mealPlanHasBeenChanged()
        _editMealPlanLiveData.value!!.mealPlan.identifier = identifier
    }

    fun getActiveItemsByType(): Map<MealType, List<MealItemWithDetails>> {
        return editMealPlanLiveData.value!!.itemsByMealType.mapValues { (_, itemList) ->
            itemList.filter { it.changeStatus != ChangeStatus.DELETED }
        }
    }

    fun deleteMealPlan() {
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.deleteMealPlanWithItems(mealPlanLiveData.value!!)
        }
    }

    fun persistMealPlan() {
        if (!isMealPlanChanged) return

        viewModelScope.launch(Dispatchers.IO) {
            val mealPlanInfo = editNutritionInfo.value!!
            val baseMealPlan = editMealPlanLiveData.value!!.mealPlan.copy(
                embeddedNutritionInfoInTotal = mealPlanInfo
            )

            val mealPlan = if (isCreatingMealPlan) {
                baseMealPlan.copy(createdAt = LocalDateTime.now())
            } else {
                baseMealPlan
            }

            val mealPlanWithItems = editMealPlanLiveData.value!!.copy(
                mealPlan = mealPlan
            )

            if (isCreatingMealPlan) {
                _mealPlanLiveData = databaseRepository.insertMealPlanWithItems(mealPlanWithItems) as MutableLiveData
                resetIsCreatingMealPlan()
            } else {
                databaseRepository.updateMealPlanWithItems(mealPlanWithItems)
            }

            resetNextNegativeId()
            resetIsMealPlanChanged()
        }
    }

    fun addMealItemsToMealPlan(mealItems: List<MealItemToAdd>, toMealType: MealType) {
        mealPlanHasBeenChanged()

        val mealTypeItems = editMealPlanLiveData.value!!.itemsByMealType[toMealType]!!

        val activeMealItems = mealTypeItems.filter { it.changeStatus != ChangeStatus.DELETED }.toMutableList()
        val deletedMealItems = mealTypeItems.filter { it.changeStatus == ChangeStatus.DELETED }

        val currentMealItemsCount = activeMealItems.size

        for ((index, mealItem) in mealItems.withIndex()) {
            activeMealItems.add(
                MealItemWithDetails(
                    id = getNextNegativeId(),
                    mealPlanId = editMealPlanLiveData.value!!.mealPlan.id,
                    mealItemId = mealItem.mealItemId,
                    mealItemType = mealItem.mealItemType,
                    sequenceIndex = index + currentMealItemsCount,
                    mealType = toMealType,
                    ingredient = mealItem.ingredient,
                    recipe = mealItem.recipe,
                    changeStatus = ChangeStatus.NEW
                )
            )
        }

        val updatedMealItemsForType = activeMealItems + deletedMealItems
        val mutableMealItemsByType = editMealPlanLiveData.value!!.itemsByMealType.toMutableMap()
        mutableMealItemsByType[toMealType] = updatedMealItemsForType
        _editMealPlanLiveData.value = editMealPlanLiveData.value!!.copy(itemsByMealType = mutableMealItemsByType)
    }

    fun deleteMealItemFromMealPlanById(mealItemId: Long, mealType: MealType) {
        mealPlanHasBeenChanged()
        val mealItemsByType = editMealPlanLiveData.value!!.itemsByMealType.toMutableMap()
        val mealItemsOfType = editMealPlanLiveData.value!!.itemsByMealType[mealType]!!.toMutableList()

        val deletedMealItem = mealItemsOfType.first { it.id == mealItemId }
        val deletedMealItemIndex = mealItemsOfType.indexOf(deletedMealItem)

        deletedMealItem.let {
            val quantity = it.quantity
            val infos = when (it.mealItemType) {
                MealItemType.INGREDIENT -> it.ingredient!!.normalisedEmbeddedNutritionInfo
                MealItemType.RECIPE -> it.recipe!!.embeddedNutritionInfoInTotal
            }

            updateNutritionInfo(-quantity, infos, mealType)
        }

        mealItemsOfType[deletedMealItemIndex] = deletedMealItem.copy(changeStatus = ChangeStatus.DELETED)

        mealItemsOfType.forEachIndexed { index, mealItem ->
            if (index > deletedMealItemIndex && mealItem.changeStatus != ChangeStatus.DELETED) {
                val decrementedIndex = mealItem.sequenceIndex - 1
                val newChangeStatus = if (mealItem.changeStatus == ChangeStatus.UNCHANGED) ChangeStatus.CHANGED else mealItem.changeStatus
                mealItemsOfType[index] = mealItem.copy(
                    sequenceIndex = decrementedIndex,
                    changeStatus = newChangeStatus
                )
            }
        }

        mealItemsByType[mealType] = mealItemsOfType
        markMealTypeUpdated(mealType)
        _editMealPlanLiveData.value = editMealPlanLiveData.value!!.copy(itemsByMealType = mealItemsByType)
    }

    private fun updateNutritionInfo(quantity: Int, nutritionInfo: EmbeddedNutritionInfo, affectedMealType: MealType) {
        updateMealPlanInfo(quantity, nutritionInfo)
        updateMealTypeInfo(quantity, nutritionInfo, affectedMealType)
    }

    private fun updateMealPlanInfo(quantity: Int, nutritionInfo: EmbeddedNutritionInfo) {
        applyItemNutritionToTotal(
            editNutritionInfo.value!!,
            nutritionInfo,
            quantity
        )
        _editNutritionInfo.value = editNutritionInfo.value
    }

    private fun updateMealTypeInfo(quantity: Int, nutritionInfo: EmbeddedNutritionInfo, affectedMealType: MealType) {
        val mealTypeInfo = editMealPlanLiveData.value!!.infoByMealType[affectedMealType]!!
        applyItemNutritionToTotal(
            mealTypeInfo,
            nutritionInfo,
            quantity
        )
        val mealTypeInfos = editMealPlanLiveData.value!!.infoByMealType.toMutableMap()
        mealTypeInfos[affectedMealType] = mealTypeInfo
        _editMealPlanLiveData.value = editMealPlanLiveData.value!!.copy(infoByMealType = mealTypeInfos)
    }

    fun updateMealItemQuantityInMealPlan(mealItemId: Long, ofMealType: MealType, newQuantity: Int) {
        mealPlanHasBeenChanged()
        val itemsByType = editMealPlanLiveData.value!!.itemsByMealType.toMutableMap()
        val itemsOfType = editMealPlanLiveData.value!!.itemsByMealType[ofMealType]!!.toMutableList()
        val mealItem = itemsOfType.first { it.id == mealItemId }
        val mealItemIndex = itemsOfType.indexOf(mealItem)

        mealItem.let {
            val quantityDifference = newQuantity - it.quantity
            val infos = when (it.mealItemType) {
                MealItemType.INGREDIENT -> it.ingredient!!.normalisedEmbeddedNutritionInfo
                MealItemType.RECIPE -> it.recipe!!.embeddedNutritionInfoInTotal
            }
            updateNutritionInfo(quantityDifference, infos, ofMealType)
        }
        val newChangeStatus = if (mealItem.changeStatus == ChangeStatus.UNCHANGED) ChangeStatus.CHANGED else mealItem.changeStatus
        itemsOfType[mealItemIndex] = mealItem.copy(changeStatus = newChangeStatus, quantity = newQuantity)
        itemsByType[ofMealType] = itemsOfType
        markMealItemUpdated(ofMealType, mealItemId)
        _editMealPlanLiveData.value = editMealPlanLiveData.value!!.copy(itemsByMealType = itemsByType)
    }
}