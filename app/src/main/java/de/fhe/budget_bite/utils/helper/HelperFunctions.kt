package de.fhe.budget_bite.utils.helper

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AlertDialog
import de.fhe.budget_bite.R
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.utils.toCalories
import de.fhe.budget_bite.utils.toPrice

object HelperFunctions {
    fun createDiscardAlertDialog(
        context: Context,
        title: String,
        onCancel: () -> Unit,
        onConfirm: () -> Unit,
        negativeButtonText: String = context.getString(R.string.button_cancel),
        positiveButtonText: String = context.getString(R.string.button_ok)
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setNegativeButton(negativeButtonText) { dialog, _ ->
                dialog.dismiss()
                onCancel()
            }
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .create()
    }

    fun showToast(context: Context, text: String, duration: Int = Toast.LENGTH_SHORT) = makeText(context, text, duration).show()

    fun setNutritionInfoView(
        context: Context,
        caloriesPriceTextView: TextView,
        nutritionInfoTextView: TextView,
        nutritionInfo: EmbeddedNutritionInfo
    ) {
        setCaloriesPriceText(
            context,
            caloriesPriceTextView,
            nutritionInfo.calories,
            nutritionInfo.price
        )

        setNutritionInfoText(
            context,
            nutritionInfoTextView,
            nutritionInfo.carbohydratesInGrams,
            nutritionInfo.fatInGrams,
            nutritionInfo.proteinInGrams
        )
    }

    fun setCaloriesPriceView(
        context: Context,
        caloriesTextView: TextView,
        priceTextView: TextView,
        calories: Float,
        price: Float
    ) {
        caloriesTextView.text = context.getString(
            R.string.format_calories,
            calories.toCalories()
        )

        priceTextView.text = context.getString(
            R.string.format_price,
            price.toPrice()
        )
    }

    private fun setCaloriesPriceText(
        context: Context,
        textView: TextView,
        calories: Float,
        price: Float
    ) {
        val caloriesText = calories.toCalories()
        val priceText = price.toPrice()
        val formattedText = context.getString(
            R.string.format_calories_price,
            caloriesText,
            context.getString(R.string.label_calories),
            priceText,
            context.getString(R.string.symbol_currency)
        )
        textView.text = formattedText
    }

    private fun setNutritionInfoText(
        context: Context,
        textView: TextView,
        carbs: Float,
        fat: Float,
        protein: Float
    ) {
        val carbsText = "${carbs}${context.getString(R.string.label_carbs)}"
        val fatText = "${fat}${context.getString(R.string.label_fat)}"
        val proteinText = "${protein}${context.getString(R.string.label_protein)}"

        textView.text = context.getString(
            R.string.format_nutrition_details,
            carbsText,
            fatText,
            proteinText
        )
    }
}