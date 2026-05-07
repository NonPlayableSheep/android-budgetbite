package de.fhe.budget_bite.storage.entity.recipe

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import java.time.LocalDateTime

@Entity
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var identifier: String = "",
    @Embedded(prefix = "nutrition_info_in_total_")
    var embeddedNutritionInfoInTotal: EmbeddedNutritionInfo = EmbeddedNutritionInfo()
)