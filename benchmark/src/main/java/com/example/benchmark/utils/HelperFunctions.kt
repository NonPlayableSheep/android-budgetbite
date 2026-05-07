package com.example.benchmark.utils

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import junit.framework.TestCase.fail

fun MacrobenchmarkScope.waitForTextShown(text: String) {
    check(device.wait(Until.hasObject(By.text(text)), 500)) {
        "View showing '$text' not found after waiting 500 ms."
    }
}

fun MacrobenchmarkScope.clickOnId(resourceId: String, isClickTrace: Boolean = false) {
    val selector = By.res(resourceId)
    if (!device.wait(Until.hasObject(selector), 2500)) {
        fail("Did not find object with id $resourceId")
    }

    device
        .findObject(selector)
        .click()

    if (isClickTrace) {
        // Chill to ensure we capture the end of the click span in the trace.
        Thread.sleep(100)
    }
}