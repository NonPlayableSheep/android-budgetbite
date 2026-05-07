package com.example.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.test.uiautomator.By
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.Direction
import com.example.benchmark.utils.Constants.DEFAULT_ITERATIONS
import com.example.benchmark.utils.Constants.PACKAGE_NAME
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    // src: https://github.com/AntonBjarne/HighDemand_XML/blob/main/app/benchmark/src/main/java/com/example/benchmark/ExampleStartupBenchmark.kt
    @Test
    fun scrollBenchmark() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = DEFAULT_ITERATIONS,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()

        scrollPostList()
    }

    private fun MacrobenchmarkScope.scrollPostList() {
        val contentList = device.findObject(By.res(packageName, "fragment_ingredients_rv"))

        device.waitForIdle()

        contentList.setGestureMargin(device.displayWidth / 5)
        contentList.scroll(Direction.DOWN, 300f)

        device.waitForIdle()
    }
}