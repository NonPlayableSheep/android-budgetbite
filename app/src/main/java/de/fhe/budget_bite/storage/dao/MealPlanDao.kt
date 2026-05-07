package de.fhe.budget_bite.storage.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import de.fhe.budget_bite.storage.entity.meal_plan.MealItem
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlan

@Dao
interface MealPlanDao {
    @Insert
    suspend fun insertMealPlan(mealPlan: MealPlan): Long

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlan)

    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlan)

    @Query("SELECT * FROM meal_plan")
    fun getAllMealPlans(): LiveData<List<MealPlan>>

    @Query("SELECT * FROM meal_plan WHERE id = :mealPlanId")
    fun getMealPlan(mealPlanId: Long): LiveData<MealPlan>

    // mealItem
    @Insert
    suspend fun insertMealItems(mealItems: List<MealItem>)

    @Update
    suspend fun updateMealItems(mealItems: List<MealItem>)

    @Delete
    suspend fun deleteMealItems(mealItems: List<MealItem>)

    @Query("SELECT * FROM meal_item WHERE meal_plan_id = :mealPlanId")
    fun getMealItems(mealPlanId: Long): LiveData<List<MealItem>>

    // info updates
    @Query("SELECT * FROM meal_item WHERE meal_item_id = :ingredientId AND meal_item_type = 'INGREDIENT'")
    suspend fun getMealItemsByIngredientId(ingredientId: Long): List<MealItem>

    @Query("SELECT * FROM meal_item WHERE meal_item_id IN (:recipeIds) AND meal_item_type = 'RECIPE'")
    suspend fun getMealItemsByRecipeIds(recipeIds: List<Long>): List<MealItem>

    @Query("SELECT * FROM meal_item WHERE meal_item_id = :recipeId AND meal_item_type = 'RECIPE'")
    suspend fun getMealItemsByRecipeId(recipeId: Long): List<MealItem>

    @Query("SELECT * FROM meal_plan WHERE id IN (:mealPlanIds)")
    suspend fun getMealPlansByIds(mealPlanIds: List<Long>): List<MealPlan>

    @Update
    suspend fun updateMealPlans(mealPlans: List<MealPlan>)
}