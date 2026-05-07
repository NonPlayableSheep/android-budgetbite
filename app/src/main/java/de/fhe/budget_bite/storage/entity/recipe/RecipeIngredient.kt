package de.fhe.budget_bite.storage.entity.recipe

import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.utils.enums.ChangeStatus

data class RecipeIngredient(
    val ingredient: Ingredient,
    var quantity: Int,
    var sequenceIndex: Int,
    var changeStatus: ChangeStatus = ChangeStatus.UNCHANGED
)
