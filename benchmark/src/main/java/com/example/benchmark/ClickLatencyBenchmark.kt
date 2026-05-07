package com.example.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import junit.framework.TestCase.fail
import androidx.test.uiautomator.Until
import com.example.benchmark.utils.Constants.DEFAULT_ITERATIONS
import com.example.benchmark.utils.Constants.PACKAGE_NAME
import com.example.benchmark.utils.clickOnId
import com.example.benchmark.utils.waitForTextShown
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * These tests measure click handling time, i.e. the time from when a touch up event is received
 * to when the click listener is fired.
 */
// src: https://github.com/android/performance-samples/blob/main/MacrobenchmarkSample/macrobenchmark/src/main/kotlin/com/example/macrobenchmark/benchmark/clickslatency/ClickLatencyBenchmark.kt
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class ClickLatencyBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun clickLatencyBenchmark() {
        var firstStart = true
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(TraceSectionMetric("ClickTrace")),
            compilationMode = CompilationMode.Full(),
            startupMode = null,
            iterations = DEFAULT_ITERATIONS,
            setupBlock = {
                if (firstStart) {
                    startActivityAndWait()
                    firstStart = false
                }
            }
        ) {
            clickOnId("de.fhe.budget_bite:id/fragment_ingredients_create_btn", true)
            waitForTextShown("Edit Ingredient")
            device.pressBack()
            waitForTextGone("Edit Ingredient")
        }
    }

    private fun MacrobenchmarkScope.waitForTextGone(text: String) {
        check(device.wait(Until.gone(By.text(text)), 500)) {
            "View showing '$text' not found after waiting 500 ms."
        }
    }
}