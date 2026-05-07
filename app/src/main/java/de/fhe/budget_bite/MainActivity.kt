package de.fhe.budget_bite

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.databinding.ActivityMainBinding
import de.fhe.budget_bite.utils.ClickTrace
import de.fhe.budget_bite.utils.gone
import de.fhe.budget_bite.utils.show
import de.fhe.budget_bite.view.detail.ingredient.DetailIngredientViewModel
import de.fhe.budget_bite.view.detail.meal_plan.DetailMealPlanViewModel
import de.fhe.budget_bite.view.detail.recipe.DetailRecipeViewModel
import de.fhe.budget_bite.view.search.add_meal_items.AddMealItemsViewModel
import de.fhe.budget_bite.view.search.ingredient.IngredientsViewModel
import de.fhe.budget_bite.view.search.mealPlan.MealPlansViewModel
import de.fhe.budget_bite.view.search.recipe.RecipesViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val sharedIngredientsViewModel: IngredientsViewModel by viewModels()
    private val sharedDetailIngredientViewModel: DetailIngredientViewModel by viewModels()
    private val sharedRecipesViewModel: RecipesViewModel by viewModels()
    private val sharedDetailRecipeViewModel: DetailRecipeViewModel by viewModels()
    private val sharedMealPlansViewModel: MealPlansViewModel by viewModels()
    private val sharedDetailMealPlanViewModel: DetailMealPlanViewModel by viewModels()
    private val sharedAddMealItemsViewModel: AddMealItemsViewModel by viewModels()
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val shouldNavigate = mainActivityViewModel.invokeNavBackFragmentCallback()
            if (shouldNavigate) {
                // Temporär deaktivieren, um Endlosschleifen zu verhindern
                this.isEnabled = false
                onBackPressedDispatcher.onBackPressed() // Navigation ausführen
                this.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ClickTrace.install()

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.activity_main_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            onNavDestinationChanged(controller, destination, arguments)
        }

        mainActivityViewModel.topLevelDestinations.observe(this) {
            appBarConfiguration = AppBarConfiguration(it)
            setupToolbarWithNavController()
        }
        mainActivityViewModel.isShowingSaveAction.observe(this) {
            invalidateOptionsMenu()
        }

        // restore ui state after rotation
        onNavDestinationChanged(navController, navController.currentDestination!!, null)

        binding.apply {
            activityMainBottomNav.setupWithNavController(navController)
            activityMainBottomNav.setOnItemSelectedListener { item ->
                NavigationUI.onNavDestinationSelected(item, navController)
                return@setOnItemSelectedListener true
            }
            setSupportActionBar(activityMainToolbar)
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupToolbarWithNavController() {
        binding.apply {
            activityMainToolbar.setupWithNavController(navController, appBarConfiguration)
            activityMainToolbar.setNavigationOnClickListener {
                // quick fix, cause it isnt fired while onOptionsItemSelected is used
                onSupportNavigateUp()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        val saveMenuItem = menu?.findItem(R.id.action_save)

        if (mainActivityViewModel.isShowingSaveAction.value!!) {
            saveMenuItem?.show()
        } else {
            saveMenuItem?.gone()
        }

        return true
    }

    // Handle menu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                mainActivityViewModel.invokeSaveActionCallback()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showActionSaveWithCallback(saveAction: () -> Unit) = mainActivityViewModel.showActionSaveWithCallback(saveAction)

    fun hideActionSave() = mainActivityViewModel.hideActionSave()

    override fun onSupportNavigateUp(): Boolean {
        val shouldNavigate = mainActivityViewModel.invokeNavBackFragmentCallback()
        return if (shouldNavigate) {
            navController.navigateUp(appBarConfiguration)
        } else {
            false
        }
    }

    fun setFragmentBackPressedCallback(callback: () -> Boolean) = mainActivityViewModel.setFragmentBackPressedCallback(callback)

    fun clearFragmentBackPressedCallback() = mainActivityViewModel.clearFragmentBackPressedCallback()

    private fun onNavDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val isTopLevelDestination = mainActivityViewModel.topLevelDestinations.value!!.contains(destination.id)
        val isAddingMealItems = sharedAddMealItemsViewModel.isAddingMealItems

        if (isTopLevelDestination && !isAddingMealItems) {
            binding.activityMainToolbar.gone()
            binding.activityMainBottomNav.show()
        } else {
            binding.activityMainToolbar.show()
            binding.activityMainBottomNav.gone()
        }
    }

    fun setSearchFragmentSecondLevelDestination() = mainActivityViewModel.setSearchFragmentSecondLevelDestination()

    fun resetSearchFragmentTopLevelDestination() = mainActivityViewModel.resetSearchFragmentTopLevelDestination()

    override fun onResume() {
        super.onResume()
        // fixes bug, that actionBar title becomes app identifier after rotation
        val currentDestination = navController.currentDestination
        currentDestination?.let {
            supportActionBar?.title = it.label
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}