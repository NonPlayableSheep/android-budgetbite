package de.fhe.budget_bite.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.fhe.budget_bite.storage.dao.IngredientDao
import de.fhe.budget_bite.storage.dao.MealPlanDao
import de.fhe.budget_bite.storage.dao.RecipeDao
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.storage.entity.meal_plan.MealItem
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlan
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.storage.entity.recipe.RecipeIngredientRef

@Database(
    entities = [
        Ingredient::class,
        Recipe::class,
        MealPlan::class,
        RecipeIngredientRef::class,
        MealItem::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun mealPlanDao(): MealPlanDao
}