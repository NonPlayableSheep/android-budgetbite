package de.fhe.budget_bite.utils.helper

import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.Ingredient
import java.math.BigDecimal
import java.math.RoundingMode

object NutritionCalculator {
    fun setNormalisedNutritionInfos(ingredient: Ingredient) {
        val measurement = BigDecimal(ingredient.measurementValueEntered)

        ingredient.normalisedEmbeddedNutritionInfo.apply {
            price = BigDecimal(ingredient.embeddedNutritionInfo.price.toString())
                .divide(measurement, 6, RoundingMode.HALF_UP)
                .toFloat()
            calories = BigDecimal(ingredient.embeddedNutritionInfo.calories.toString())
                .divide(measurement, 6, RoundingMode.HALF_UP)
                .toFloat()
            carbohydratesInGrams = BigDecimal(ingredient.embeddedNutritionInfo.carbohydratesInGrams.toString())
                .divide(measurement, 6, RoundingMode.HALF_UP)
                .toFloat()
            fatInGrams = BigDecimal(ingredient.embeddedNutritionInfo.fatInGrams.toString())
                .divide(measurement, 6, RoundingMode.HALF_UP)
                .toFloat()
            proteinInGrams = BigDecimal(ingredient.embeddedNutritionInfo.proteinInGrams.toString())
                .divide(measurement, 6, RoundingMode.HALF_UP)
                .toFloat()
        }
    }

    fun applyItemNutritionToTotal(
        totalNutritionInfo: EmbeddedNutritionInfo,
        itemNutritionInfo: EmbeddedNutritionInfo,
        quantity: Int
    ) {
        totalNutritionInfo.apply {
            calories += itemNutritionInfo.calories * quantity
            price += itemNutritionInfo.price * quantity
            carbohydratesInGrams += itemNutritionInfo.carbohydratesInGrams * quantity
            fatInGrams += itemNutritionInfo.fatInGrams * quantity
            proteinInGrams += itemNutritionInfo.proteinInGrams * quantity
        }
    }
}