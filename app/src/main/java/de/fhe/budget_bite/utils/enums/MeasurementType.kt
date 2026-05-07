package de.fhe.budget_bite.utils.enums

import androidx.annotation.StringRes
import de.fhe.budget_bite.R

enum class MeasurementType(@StringRes val displayString: Int) {
    WEIGHT(R.string.measurement_type_weight),
    VOLUME(R.string.measurement_type_volume),
    QUANTITY(R.string.measurement_type_quantity),
}