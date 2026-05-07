package de.fhe.budget_bite.storage.entity.recipe

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import de.fhe.budget_bite.storage.entity.Ingredient

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(
        parentColumn = "id", // Recipe.id
        entityColumn = "id", // RecipeIngredientRef.ingredientId
        associateBy = Junction(
            value = RecipeIngredientRef::class,
            parentColumn = "recipe_id", // RecipeIngredientRef.recipeId
            entityColumn = "ingredient_id" // RecipeIngredientRef.ingredientId
        )
    )
    val ingredients: List<Ingredient>
)
