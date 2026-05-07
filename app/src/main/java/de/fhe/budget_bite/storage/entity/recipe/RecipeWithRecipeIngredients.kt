package de.fhe.budget_bite.storage.entity.recipe

data class RecipeWithRecipeIngredients(
    val recipe: Recipe = Recipe(),
    var ingredients: List<RecipeIngredient> = listOf()
)
