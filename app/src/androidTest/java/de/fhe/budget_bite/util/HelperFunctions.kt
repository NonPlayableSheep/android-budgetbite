package de.fhe.budget_bite.util

import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemWithDetails
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlan
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlanWithItems
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.utils.enums.ChangeStatus
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType

object HelperFunctions {
    fun getMealPlanWithItems(recipe: Recipe, quantity: Int): MealPlanWithItems {
        // Berechne die EmbeddedNutritionInfo für den MealPlan basierend auf dem Rezept und der Menge
        val embeddedNutritionInfo = EmbeddedNutritionInfo(
            price = recipe.embeddedNutritionInfoInTotal.price * quantity,
            calories = recipe.embeddedNutritionInfoInTotal.calories * quantity,
            carbohydratesInGrams = recipe.embeddedNutritionInfoInTotal.carbohydratesInGrams * quantity,
            fatInGrams = recipe.embeddedNutritionInfoInTotal.fatInGrams * quantity,
            proteinInGrams = recipe.embeddedNutritionInfoInTotal.proteinInGrams * quantity
        )

// Erstellen des MealPlan mit den berechneten NutritionInfos
        val mealPlan = MealPlan(
            identifier = "Test Meal Plan",
            embeddedNutritionInfoInTotal = embeddedNutritionInfo
        )

        // Definieren des MealTypes, hier BREAKFAST als Beispiel
        val mealType = MealType.BREAKFAST

        // Erstellen eines MealItemWithDetails, welches das Rezept als MealItem enthält
        val mealItemRecipe = MealItemWithDetails(
            mealItemId = recipe.id,
            quantity = quantity,
            sequenceIndex = 1,
            mealType = mealType,
            mealItemType = MealItemType.RECIPE,
            recipe = recipe,
            changeStatus = ChangeStatus.NEW
        )

        // Zuweisen der MealItems zum entsprechenden MealType
        val itemByType = mutableMapOf<MealType, List<MealItemWithDetails>>()
        itemByType[mealType] = listOf(mealItemRecipe)

        // Erstellen des MealPlanWithItems-Objekts
        return MealPlanWithItems(
            mealPlan = mealPlan,
            itemsByMealType = itemByType.toMap()
        )
    }
}