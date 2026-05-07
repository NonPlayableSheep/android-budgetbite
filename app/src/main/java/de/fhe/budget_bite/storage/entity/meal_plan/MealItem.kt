package de.fhe.budget_bite.storage.entity.meal_plan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType

@Entity(tableName = "meal_item")
data class MealItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "meal_plan_id")
    val mealPlanId: Long,
    @ColumnInfo(name = "meal_item_id")
    val mealItemId: Long, // ID of the associated recipe or ingredient
    val quantity: Int = 0,
    @ColumnInfo(name = "sequence_index")
    val sequenceIndex: Int = 0,
    @ColumnInfo(name = "meal_type")
    val mealType: MealType,
    @ColumnInfo(name = "meal_item_type")
    val mealItemType: MealItemType // Specifies if the item is a recipe or an ingredient
)
