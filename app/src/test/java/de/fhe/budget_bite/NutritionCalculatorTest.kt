package de.fhe.budget_bite

import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.utils.helper.NutritionCalculator.applyItemNutritionToTotal
import de.fhe.budget_bite.utils.helper.NutritionCalculator.setNormalisedNutritionInfos
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {
    @Test
    fun setNormalisedNutritionInfos_dividesCorrectly() {
        val ingredient = Ingredient(
            identifier = "Test Ingredient",
            measurementValueEntered = 5,
            embeddedNutritionInfo = EmbeddedNutritionInfo(
                price = 10.0f,
                calories = 500.0f,
                carbohydratesInGrams = 100.0f,
                fatInGrams = 50.0f,
                proteinInGrams = 30.0f
            ),
            normalisedEmbeddedNutritionInfo = EmbeddedNutritionInfo() // Initialize with empty values
        )

        setNormalisedNutritionInfos(ingredient)

        with(ingredient.normalisedEmbeddedNutritionInfo) {
            assertEquals(2.0f, price, 0.000001f)
            assertEquals(100.0f, calories, 0.000001f)
            assertEquals(20.0f, carbohydratesInGrams, 0.000001f)
            assertEquals(10.0f, fatInGrams, 0.000001f)
            assertEquals(6.0f, proteinInGrams, 0.000001f)
        }
    }

    @Test
    fun applyItemNutritionToTotal_addsCorrectly() {
        // Gegeben: Ein TotalNutritionInfo mit Anfangswerten und ein ItemNutritionInfo
        val totalNutritionInfo = EmbeddedNutritionInfo(
            price = 0.0f,
            calories = 0.0f,
            carbohydratesInGrams = 0.0f,
            fatInGrams = 0.0f,
            proteinInGrams = 0.0f
        )
        val itemNutritionInfo = EmbeddedNutritionInfo(
            price = 2.0f,
            calories = 100.0f,
            carbohydratesInGrams = 20.0f,
            fatInGrams = 10.0f,
            proteinInGrams = 5.0f
        )
        val quantity = 3

        // Wenn: applyItemNutritionToTotal aufgerufen wird
        applyItemNutritionToTotal(totalNutritionInfo, itemNutritionInfo, quantity)

        // Dann: Die Werte des totalNutritionInfo sollten korrekt addiert werden
        assertEquals(6.0f, totalNutritionInfo.price)
        assertEquals(300.0f, totalNutritionInfo.calories)
        assertEquals(60.0f, totalNutritionInfo.carbohydratesInGrams)
        assertEquals(30.0f, totalNutritionInfo.fatInGrams)
        assertEquals(15.0f, totalNutritionInfo.proteinInGrams)
    }

    @Test
    fun applyItemNutritionToTotal_subtractsCorrectly() {
        // Gegeben: Ein TotalNutritionInfo mit Anfangswerten und ein ItemNutritionInfo
        val totalNutritionInfo = EmbeddedNutritionInfo(
            price = 10.0f,
            calories = 500.0f,
            carbohydratesInGrams = 100.0f,
            fatInGrams = 50.0f,
            proteinInGrams = 30.0f
        )
        val itemNutritionInfo = EmbeddedNutritionInfo(
            price = 2.0f,
            calories = 100.0f,
            carbohydratesInGrams = 20.0f,
            fatInGrams = 10.0f,
            proteinInGrams = 5.0f
        )
        val quantity = -2

        // Wenn: applyItemNutritionToTotal aufgerufen wird mit negativer Menge (Subtraktion)
        applyItemNutritionToTotal(totalNutritionInfo, itemNutritionInfo, quantity)

        // Dann: Die Werte des totalNutritionInfo sollten korrekt subtrahiert werden
        assertEquals(6.0f, totalNutritionInfo.price)
        assertEquals(300.0f, totalNutritionInfo.calories)
        assertEquals(60.0f, totalNutritionInfo.carbohydratesInGrams)
        assertEquals(30.0f, totalNutritionInfo.fatInGrams)
        assertEquals(20.0f, totalNutritionInfo.proteinInGrams)
    }
}