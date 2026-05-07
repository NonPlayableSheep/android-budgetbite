package de.fhe.budget_bite.view.search.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.fhe.budget_bite.databinding.ListItemSearchBinding
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.gone
import de.fhe.budget_bite.utils.helper.HelperFunctions
import de.fhe.budget_bite.utils.show

class RecipesAdapter(
    private val onItemClick: (Long) -> Unit,
    private val isAddingMealItems: Boolean,
    private val isMealItemSelected: (Long, MealItemType) -> Boolean,
    private var dataset: MutableList<Recipe> = mutableListOf(),
) : RecyclerView.Adapter<RecipesAdapter.ItemViewHolder>() {
    class ItemViewHolder(
        private val binding: ListItemSearchBinding,
        private val isMealItemSelected: (Long, MealItemType) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindData(recipe: Recipe, isAddingMealItems: Boolean) {
            binding.apply {
                listItemSearchIdentifier.text = recipe.identifier

                val calories = recipe.embeddedNutritionInfoInTotal.calories
                val price = recipe.embeddedNutritionInfoInTotal.price

                HelperFunctions.setCaloriesPriceView(
                    binding.root.context,
                    listItemSearchCalories,
                    listItemSearchPrice,
                    calories,
                    price
                )

                listItemSearchAddIngredientBtn.gone()
                listItemSearchUncheckIngredientBtn.gone()
                when {
                    isAddingMealItems && isMealItemSelected(recipe.id, MealItemType.RECIPE) -> listItemSearchUncheckIngredientBtn.show()
                    isAddingMealItems -> listItemSearchAddIngredientBtn.show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemSearchBinding.inflate(inflater, parent, false)
        return ItemViewHolder(binding, isMealItemSelected)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val recipe = dataset[position]
        holder.bindData(recipe, isAddingMealItems)
        holder.itemView.setOnClickListener {
            onItemClick(recipe.id)
        }
    }

    fun updateDataset(dataUpdate: List<Recipe>) {
        dataset.clear()
        dataset.addAll(dataUpdate)
        notifyDataSetChanged()
    }
}