package de.fhe.budget_bite.utils.enums

import androidx.annotation.StringRes
import de.fhe.budget_bite.R

enum class SearchViewPagerTab(val position: Int, @StringRes val title: Int) {
    DUMMY(0, R.string.tab_all),
    INGREDIENTS(1, R.string.tab_ingredients),
    RECIPES(2, R.string.tab_recipes),
    MEAL_PLANS(3, R.string.tab_meal_plans);

    companion object {
        fun getTabs(isAddingMealItems: Boolean): List<SearchViewPagerTab> {
            return if (isAddingMealItems) {
                listOf(DUMMY, INGREDIENTS, RECIPES)
            } else {
                listOf(DUMMY, INGREDIENTS, RECIPES, MEAL_PLANS)
            }
        }

        fun fromPosition(position: Int): SearchViewPagerTab? {
            return entries.find { it.position == position }
        }
    }
}
