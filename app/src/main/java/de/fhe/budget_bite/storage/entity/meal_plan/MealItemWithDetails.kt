package de.fhe.budget_bite.storage.entity.meal_plan

import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.utils.enums.ChangeStatus
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType

data class MealItemWithDetails(
    val id: Long = 0, // Eindeutige ID des MealItem
    val mealPlanId: Long = 0, // Verknüpfung zum Meal Plan
    val mealItemId: Long = 0, // ID des Rezepts oder der Zutat
    val quantity: Int = 0, // Menge des Items im Meal Plan
    val sequenceIndex: Int = -1, // Reihenfolge im Meal Plan
    val mealType: MealType, // Typ des Meals (z.B. Frühstück, Mittagessen, etc.)
    val mealItemType: MealItemType, // Differenziert, ob es ein Rezept oder eine Zutat ist
    val recipe: Recipe? = null, // Nur gesetzt, wenn MealItemType.RECIPE
    val ingredient: Ingredient? = null, // Nur gesetzt, wenn MealItemType.INGREDIENT
    val changeStatus: ChangeStatus = ChangeStatus.UNCHANGED
)

