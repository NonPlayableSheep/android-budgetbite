package de.fhe.budget_bite.view.search

import androidx.lifecycle.ViewModel
import de.fhe.budget_bite.utils.enums.SearchViewPagerTab

class SearchViewModel : ViewModel() {
    private var _selectedTabPosition = SearchViewPagerTab.INGREDIENTS.position
    val selectedTabPosition get() = _selectedTabPosition

    fun setSelectedTabPosition(position: Int) {
        _selectedTabPosition = position
    }

    fun setSelectedTabPositionToStart() {
        _selectedTabPosition = SearchViewPagerTab.INGREDIENTS.position
    }

    fun setSelectedTabPositionToMealPlans() {
        _selectedTabPosition = SearchViewPagerTab.MEAL_PLANS.position
    }
}