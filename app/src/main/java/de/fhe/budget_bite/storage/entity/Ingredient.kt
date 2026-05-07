package de.fhe.budget_bite.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.fhe.budget_bite.utils.enums.MeasurementType
import java.time.LocalDateTime

@Entity
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var identifier: String = "",
    @ColumnInfo(name = "measurement_type")
    var measurementType: MeasurementType = MeasurementType.WEIGHT,
    @ColumnInfo(name = "measurement_value_entered")
    var measurementValueEntered: Int = 1,
    @Embedded(prefix = "normalised_nutrition_info_")
    var normalisedEmbeddedNutritionInfo: EmbeddedNutritionInfo = EmbeddedNutritionInfo(),
    @Embedded(prefix = "nutrition_info_")
    var embeddedNutritionInfo: EmbeddedNutritionInfo = EmbeddedNutritionInfo(),
)
