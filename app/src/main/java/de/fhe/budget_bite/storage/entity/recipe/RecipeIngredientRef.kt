package de.fhe.budget_bite.storage.entity.recipe

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["recipe_id", "ingredient_id"])
data class RecipeIngredientRef(
    @ColumnInfo(name = "recipe_id")
    val recipeId: Long,
    @ColumnInfo(name = "ingredient_id")
    val ingredientId: Long,
    val quantity: Int = 0,
    @ColumnInfo(name = "sequence_index")
    val sequenceIndex: Int
)

