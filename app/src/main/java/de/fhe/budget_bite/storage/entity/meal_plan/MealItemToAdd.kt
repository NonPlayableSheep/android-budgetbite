package de.fhe.budget_bite.storage.entity.meal_plan

import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.utils.enums.MealItemType

data class MealItemToAdd(
    val mealItemId: Long,
    val mealItemType: MealItemType,
    val ingredient: Ingredient? = null,
    val recipe: Recipe? = null
)
