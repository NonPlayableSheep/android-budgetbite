package de.fhe.budget_bite.storage.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import de.fhe.budget_bite.storage.entity.Ingredient

@Dao
interface IngredientDao {
    @Insert
    suspend fun insertIngredient(ingredient: Ingredient): Long

    @Update
    suspend fun updateIngredient(ingredient: Ingredient)

    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)

    @Query("SELECT * FROM ingredient")
    fun getAllIngredients(): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredient WHERE id NOT IN (:ingredientIds)")
    fun getIngredientsNotInList(ingredientIds: List<Long>): LiveData<List<Ingredient>>

    @Query("SELECT * FROM ingredient WHERE id = :ingredientId")
    fun getIngredientLiveDataById(ingredientId: Long): LiveData<Ingredient>

    // for meal plan
    @Query("SELECT * FROM ingredient WHERE id IN (:ingredientIds)")
    fun getIngredientsByIds(ingredientIds: List<Long>): LiveData<List<Ingredient>>

    // infos
    @Query("SELECT * FROM ingredient WHERE id = :ingredientId")
    fun getIngredientById(ingredientId: Long): Ingredient

    // test data
    @Query("SELECT * FROM ingredient LIMIT 1")
    suspend fun checkForEmptyTable(): Ingredient?

    @Insert
    suspend fun insertTestIngredients(ingredients: List<Ingredient>)

    @Query("SELECT * FROM ingredient")
    suspend fun getAllTestIngredients(): List<Ingredient>
}