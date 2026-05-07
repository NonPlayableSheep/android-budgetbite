package com.example.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import com.example.benchmark.utils.Constants.DEFAULT_ITERATIONS
import com.example.benchmark.utils.Constants.PACKAGE_NAME
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    // src: vgl.3, but manuscript
    @Test
    fun bottomNavigationBenchmark() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = DEFAULT_ITERATIONS,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
        repeatNavigateScreens()
    }

    private fun MacrobenchmarkScope.repeatNavigateScreens() {
        repeat(10) {
            device.findObject(By.text("Planner")).click()
            device.findObject(By.text("Search")).click()
            device.findObject(By.text("More")).click()
        }
    }
}