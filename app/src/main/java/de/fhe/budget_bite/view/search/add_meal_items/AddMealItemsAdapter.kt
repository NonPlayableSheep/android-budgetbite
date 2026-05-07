package de.fhe.budget_bite.view.search.add_meal_items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.fhe.budget_bite.databinding.ListItemSearchBinding
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemToAdd
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.gone
import de.fhe.budget_bite.utils.helper.HelperFunctions.setCaloriesPriceView
import de.fhe.budget_bite.utils.show

class AddMealItemsAdapter(
    private val onItemClick: (Int, Long, MealItemType) -> Unit,
    private val isItemSelected: (Long, MealItemType) -> Boolean,
    private var dataset: List<MealItemToAdd> = listOf()
) : RecyclerView.Adapter<AddMealItemsAdapter.ItemViewHolder>() {
    class ItemViewHolder(
        private val binding: ListItemSearchBinding,
        private val isItemSelected: (Long, MealItemType) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(mealItemToAdd: MealItemToAdd) {
            binding.apply {
                val mealItemId: Long
                val mealItemType: MealItemType
                val identifier: String
                val calories: Float
                val price: Float

                when (mealItemToAdd.mealItemType) {
                    MealItemType.INGREDIENT -> {
                        val ingredient = mealItemToAdd.ingredient!!
                        ingredient.let {
                            mealItemId = it.id
                            mealItemType = MealItemType.INGREDIENT
                            identifier = it.identifier
                            calories = it.embeddedNutritionInfo.calories
                            price = it.embeddedNutritionInfo.price
                        }
                    }
                    MealItemType.RECIPE -> {
                        val recipe = mealItemToAdd.recipe!!
                        recipe.let {
                            mealItemId = it.id
                            mealItemType = MealItemType.RECIPE
                            identifier = it.identifier
                            calories = it.embeddedNutritionInfoInTotal.calories
                            price = it.embeddedNutritionInfoInTotal.price
                        }
                    }
                }

                listItemSearchIdentifier.text = identifier
                setCaloriesPriceView(
                    binding.root.context,
                    listItemSearchCalories,
                    listItemSearchPrice,
                    calories,
                    price
                )

                listItemSearchAddIngredientBtn.gone()
                listItemSearchUncheckIngredientBtn.gone()
                if (isItemSelected(mealItemId, mealItemType)) {
                    listItemSearchUncheckIngredientBtn.show()
                } else {
                    listItemSearchAddIngredientBtn.show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemSearchBinding.inflate(inflater, parent, false)
        return ItemViewHolder(binding, isItemSelected)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val mealItemToAdd = dataset[position]
        holder.bindData(mealItemToAdd)
        holder.itemView.setOnClickListener {
            onItemClick(position, mealItemToAdd.mealItemId, mealItemToAdd.mealItemType)
        }
    }

    fun updateDataset(dataUpdate: List<MealItemToAdd>) {
        dataset = dataUpdate
        notifyDataSetChanged()
    }
}