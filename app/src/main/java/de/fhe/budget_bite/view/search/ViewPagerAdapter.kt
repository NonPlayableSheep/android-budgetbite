package de.fhe.budget_bite.view.search

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.fhe.budget_bite.utils.enums.SearchViewPagerTab
import de.fhe.budget_bite.view.search.ingredient.IngredientsFragment
import de.fhe.budget_bite.view.search.mealPlan.MealPlansFragment
import de.fhe.budget_bite.view.search.recipe.RecipesFragment

class ViewPagerAdapter(
    fragment: Fragment,
    private val isAddingMealItems: () -> Boolean
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return if (isAddingMealItems()) {
            SearchViewPagerTab.RECIPES.position + 1
        } else {
            SearchViewPagerTab.MEAL_PLANS.position + 1
        }
    }

    override fun createFragment(position: Int): Fragment {
        return when (SearchViewPagerTab.fromPosition(position)) {
            SearchViewPagerTab.DUMMY -> ViewPagerDummyFragment()
            SearchViewPagerTab.INGREDIENTS -> IngredientsFragment()
            SearchViewPagerTab.RECIPES -> RecipesFragment()
            SearchViewPagerTab.MEAL_PLANS -> MealPlansFragment()
            null -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}