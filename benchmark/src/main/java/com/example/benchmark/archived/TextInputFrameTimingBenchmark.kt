package com.example.benchmark.archived

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import com.example.benchmark.utils.Constants.DEFAULT_ITERATIONS
import com.example.benchmark.utils.Constants.PACKAGE_NAME
import com.example.benchmark.utils.clickOnId
import com.example.benchmark.utils.waitForTextShown
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// src:
@RunWith(AndroidJUnit4::class)
class TextInputFrameTimingBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun inputText() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            // Try switching to different compilation modes to see the effect
            // it has on frame timing metrics.
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.WARM, // restarts activity each iteration
            iterations = DEFAULT_ITERATIONS,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                clickOnId("de.fhe.budget_bite:id/fragment_ingredients_create_btn")
                waitForTextShown("Edit Ingredient")
            }
        ) {
            val input = device.findObject(By.res("de.fhe.budget_bite:id/fragment_detail_ingredient_edit_identifier"))
            repeat(3) {
                input.text = ""
                input.text = "Hello World"
            }
        }
    }
}