package de.fhe.budget_bite.storage.entity

import androidx.room.ColumnInfo

data class EmbeddedNutritionInfo(
    var price: Float = 0.0f,
    var calories: Float = 0.0f,
    @ColumnInfo(name = "fat_in_grams")
    var fatInGrams: Float = 0.0f,
    @ColumnInfo(name = "carbohydrates_in_grams")
    var carbohydratesInGrams: Float = 0.0f,
    @ColumnInfo(name = "protein_in_grams")
    var proteinInGrams: Float = 0.0f
)
