package de.fhe.budget_bite.storage.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.storage.entity.recipe.RecipeIngredientRef
import de.fhe.budget_bite.storage.entity.recipe.RecipeWithIngredients

@Dao
interface RecipeDao {
    @Insert
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("SELECT * FROM recipe")
    fun getAllRecipes(): LiveData<List<Recipe>>

    @Transaction
    @Query("SELECT * FROM recipe WHERE id = :recipeId")
    fun getRecipeWithIngredients(recipeId: Long): LiveData<RecipeWithIngredients>


    // for meal plan
    @Query("SELECT * FROM recipe WHERE id IN (:recipeIds)")
    fun getRecipesByIds(recipeIds: List<Long>): LiveData<List<Recipe>>


    // for nutrition info operations
    @Query("SELECT * FROM recipe WHERE id = :recipeId")
    suspend fun getRecipeById(recipeId: Long): Recipe

    @Query("""
    SELECT r.* FROM recipe r
    INNER JOIN RecipeIngredientRef ref ON r.id = ref.recipe_id
    WHERE ref.ingredient_id = :ingredientId
    """)
    suspend fun getRecipesByIngredient(ingredientId: Long): List<Recipe>

    @Update
    suspend fun updateRecipes(recipes: List<Recipe>)

    @Query("SELECT * FROM RecipeIngredientRef WHERE ingredient_id = :ingredientId")
    suspend fun getRecipeIngredientRefsByIngredient(ingredientId: Long): List<RecipeIngredientRef>

    @Query("SELECT * FROM RecipeIngredientRef WHERE recipe_id IN (:recipeIds)")
    suspend fun getRecipeIngredientRefsForRecipes(recipeIds: List<Long>): List<RecipeIngredientRef>


    // RecipeIngredientCrossRef
    @Insert
    suspend fun insertRecipeIngredientRefs(recipeIngredientRefs: List<RecipeIngredientRef>): List<Long>

    @Update
    suspend fun updateRecipeIngredientRefs(recipeIngredientRef: List<RecipeIngredientRef>)

    @Delete
    suspend fun deleteRecipeIngredientRefs(recipeIngredientRef: List<RecipeIngredientRef>)

    @Query("DELETE FROM recipeIngredientRef WHERE recipe_id = :recipeId")
    suspend fun deleteRecipeIngredientRefsByRecipeId(recipeId: Long)

    @Query("SELECT * FROM recipeIngredientRef WHERE recipe_id = :recipeId ORDER BY sequence_index" +
                   " ASC")
    fun getRecipeIngredientRefs(recipeId: Long): LiveData<List<RecipeIngredientRef>>


    // test data
    @Query("SELECT * FROM recipe")
    suspend fun getAllTestRecipes(): List<Recipe>
}