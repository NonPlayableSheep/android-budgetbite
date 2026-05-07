package de.fhe.budget_bite.view.search.mealPlan

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.fhe.budget_bite.storage.DatabaseRepository
import javax.inject.Inject

@HiltViewModel
class MealPlansViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepository
) : ViewModel() {
    val mealPlanLiveData = databaseRepository.getAllMealPlans()
}