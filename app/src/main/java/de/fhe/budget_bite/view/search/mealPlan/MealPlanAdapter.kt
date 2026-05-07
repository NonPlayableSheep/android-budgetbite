package de.fhe.budget_bite.view.search.mealPlan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.fhe.budget_bite.databinding.ListItemMealPlanBinding
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlan
import de.fhe.budget_bite.utils.helper.HelperFunctions.setCaloriesPriceView

class MealPlanAdapter(
    private val onItemClick: (Int) -> Unit,
    private var dataset: MutableList<MealPlan> = mutableListOf()
) : RecyclerView.Adapter<MealPlanAdapter.ItemViewHolder>() {
    class ItemViewHolder(private val binding: ListItemMealPlanBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bindData(mealPlan: MealPlan) {
                binding.apply {
                    listItemMealPlanIdentifier.text = mealPlan.identifier

                    val calories = mealPlan.embeddedNutritionInfoInTotal.calories
                    val price = mealPlan.embeddedNutritionInfoInTotal.price

                    setCaloriesPriceView(
                        binding.root.context,
                        listItemMealPlanCalories,
                        listItemMealPlanPrice,
                        calories,
                        price
                    )
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemMealPlanBinding.inflate(inflater, parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val mealPlan = dataset[position]
        holder.bindData(mealPlan)
        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    fun updateDataset(dataUpdate: List<MealPlan>) {
        dataset.clear()
        dataset.addAll(dataUpdate)
        notifyDataSetChanged()
    }
}