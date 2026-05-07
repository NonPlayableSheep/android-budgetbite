package de.fhe.budget_bite.view.search.ingredient

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.fhe.budget_bite.databinding.ListItemSearchBinding
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.gone
import de.fhe.budget_bite.utils.helper.HelperFunctions.setCaloriesPriceView
import de.fhe.budget_bite.utils.show

class IngredientsAdapter(
    private val onItemClick: (Int, Long, Boolean) -> Unit,
    private val isIngredientClicked: (Long) -> Boolean,
    private val isAddingIngredients: Boolean,
    private val isFromDialog: Boolean = false,
    private val isAddingMealItems: Boolean,
    private val isMealItemSelected: (Long, MealItemType) -> Boolean,
    private var dataset: MutableList<Ingredient> = mutableListOf(),
) : RecyclerView.Adapter<IngredientsAdapter.ItemViewHolder>() {
    class ItemViewHolder(
        private val binding: ListItemSearchBinding,
        private val isIngredientClicked: (Long) -> Boolean,
        private val isMealItemSelected: (Long, MealItemType) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindData(ingredient: Ingredient, isAddingIngredients: Boolean, isAddingMealItems: Boolean) {
            binding.apply {
                listItemSearchIdentifier.text = ingredient.identifier

                val calories = ingredient.embeddedNutritionInfo.calories
                val price = ingredient.embeddedNutritionInfo.price

                setCaloriesPriceView(
                    binding.root.context,
                    listItemSearchCalories,
                    listItemSearchPrice,
                    calories,
                    price
                )

                listItemSearchAddIngredientBtn.gone()
                listItemSearchUncheckIngredientBtn.gone()

                when {
                    isAddingIngredients && isIngredientClicked(ingredient.id) -> listItemSearchUncheckIngredientBtn.show()
                    isAddingMealItems && isMealItemSelected(ingredient.id, MealItemType.INGREDIENT) -> listItemSearchUncheckIngredientBtn.show()
                    isAddingIngredients || isAddingMealItems -> listItemSearchAddIngredientBtn.show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemSearchBinding.inflate(inflater, parent, false)
        return ItemViewHolder(binding, isIngredientClicked, isMealItemSelected)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val ingredient = dataset[position]
        holder.bindData(ingredient, isAddingIngredients, isAddingMealItems)
        holder.itemView.setOnClickListener {
            onItemClick(position, ingredient.id, isFromDialog)
        }
    }

    fun updateDataset(dataUpdate: List<Ingredient>) {
        dataset.clear()
        dataset.addAll(dataUpdate)
        notifyDataSetChanged()
    }
}