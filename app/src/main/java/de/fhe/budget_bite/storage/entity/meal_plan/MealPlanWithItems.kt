package de.fhe.budget_bite.storage.entity.meal_plan

import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.utils.enums.MealType

data class MealPlanWithItems(
    val mealPlan: MealPlan = MealPlan(),
    val itemsByMealType: Map<MealType, List<MealItemWithDetails>> = initEmptyItemsByMealType(),
    val infoByMealType: Map<MealType, EmbeddedNutritionInfo> = initEmptyInfoByMealType()
) {
    companion object {
        fun initEmptyItemsByMealType(): Map<MealType, List<MealItemWithDetails>> {
            val allMealTypes = MealType.entries.toSet()
            return allMealTypes.associateWith { emptyList<MealItemWithDetails>() }
        }

        fun initEmptyInfoByMealType(): Map<MealType, EmbeddedNutritionInfo> {
            val allMealTypes = MealType.entries.toSet()
            return allMealTypes.associateWith { EmbeddedNutritionInfo() }
        }
    }
}
