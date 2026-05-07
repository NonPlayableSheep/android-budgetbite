package de.fhe.budget_bite.view.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.FragmentSearchBinding
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.SearchViewPagerTab
import de.fhe.budget_bite.utils.gone
import de.fhe.budget_bite.utils.helper.HelperFunctions
import de.fhe.budget_bite.utils.requireMainActivity
import de.fhe.budget_bite.utils.show
import de.fhe.budget_bite.view.detail.meal_plan.DetailMealPlanViewModel
import de.fhe.budget_bite.view.search.add_meal_items.AddMealItemsAdapter
import de.fhe.budget_bite.view.search.add_meal_items.AddMealItemsViewModel
import de.fhe.budget_bite.view.search.ingredient.IngredientsViewModel
import de.fhe.budget_bite.view.search.recipe.RecipesViewModel

@AndroidEntryPoint
class SearchFragment : Fragment(R.layout.fragment_search) {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var searchViewModel: SearchViewModel
    private var _addMealItemsBottomSheet: BottomSheetDialog? = null
    private val addMealItemsBottomSheet get() = _addMealItemsBottomSheet!!
    private var _addMealItemsAdapter: AddMealItemsAdapter? = null
    private val addMealItemsAdapter get() = _addMealItemsAdapter!!
    private val addMealItemsViewModel: AddMealItemsViewModel by activityViewModels()
    private val ingredientsViewModel: IngredientsViewModel by activityViewModels()
    private val recipesViewModel: RecipesViewModel by activityViewModels()
    private val detailMealPlanViewModel: DetailMealPlanViewModel by activityViewModels()
    private val onPageChange = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            searchViewModel.setSelectedTabPosition(position)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchViewModel = ViewModelProvider(requireActivity())[SearchViewModel::class.java]

        viewPagerAdapter = ViewPagerAdapter(this, ::isAddingMealItems)
        viewPager = binding.fragmentSearchViewPager
        viewPager.adapter = viewPagerAdapter
        viewPager.registerOnPageChangeCallback(onPageChange)

        val tabLayout = binding.fragmentSearchTabLayout
        val tabs = SearchViewPagerTab.getTabs(isAddingMealItems())
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val currentTab = tabs[position]
            tab.text = getString(currentTab.title)
        }.attach()

        if (isAddingMealItems()) {
            searchViewModel.setSelectedTabPositionToStart()
        }

        // set last tab on nav back
        val position = searchViewModel.selectedTabPosition
        tabLayout.getTabAt(position)?.select()
        viewPager.setCurrentItem(position, false)

        binding.apply {
            if (isAddingMealItems()) {
                setBackPressedCallback()

                initAddMealItemsBottomSheet()
                fragmentSearchAddPreviewLayout.setOnClickListener {
                    openAddMealItemsBottomSheet()
                }

                fragmentSearchAddBtn.setOnClickListener {
                    addMealItemsViewModel.let {
                        detailMealPlanViewModel.addMealItemsToMealPlan(
                            it.getSelectedMealItemsToAdd(),
                            it.getAddingMealType()
                        )

                        it.resetAdding()
                    }
                    searchViewModel.setSelectedTabPositionToMealPlans()
                    requireMainActivity().apply {
                        resetSearchFragmentTopLevelDestination()
                        clearFragmentBackPressedCallback()
                    }

                    findNavController().popBackStack(R.id.detailMealPlanEditFragment, false)
                }

                addMealItemsViewModel.selectedMealItems.observe(viewLifecycleOwner) {
                    addMealItemsAdapter.notifyDataSetChanged()

                    val count = it.size
                    fragmentSearchAddCountTv.text = count.toString()

                    if (addMealItemsViewModel.hasAddedFirstItem) {
                        fragmentSearchAddLayout.show()
                    }
                    if (it.isEmpty()) {
                        fragmentSearchAddLayout.gone()
                    }
                }
            }
        }
    }

    private fun setBackPressedCallback() {
        requireMainActivity().apply {
            setFragmentBackPressedCallback {
                val selectedMealItems = addMealItemsViewModel.selectedMealItems.value!!
                if (selectedMealItems.isNotEmpty()) {
                    openDiscardAddingAlert()
                    false
                } else {
                    resetSearchFragmentTopLevelDestination()
                    addMealItemsViewModel.resetAdding()
                    searchViewModel.setSelectedTabPositionToMealPlans()
                    true
                }
            }
        }
    }

    private fun initAddMealItemsBottomSheet() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bottom_sheet_add_items, null)
        _addMealItemsBottomSheet = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        addMealItemsBottomSheet.setContentView(dialogView)
        val bottomSheetRecyclerView = dialogView.findViewById<RecyclerView>(R.id.dialog_bottom_sheet_add_items_rv)
        _addMealItemsAdapter = AddMealItemsAdapter(
            ::onItemClick,
            addMealItemsViewModel::isMealItemSelected
        )
        bottomSheetRecyclerView.adapter = addMealItemsAdapter
    }

    private fun openAddMealItemsBottomSheet() {
        addMealItemsAdapter.updateDataset(
            addMealItemsViewModel.getSelectedMealItemsToAdd()
        )
        addMealItemsBottomSheet.show()
    }

    private fun onItemClick(position: Int, mealItemId: Long, mealItemType: MealItemType) {
        addMealItemsAdapter.notifyItemChanged(position)
        toggleMealItem(mealItemType, mealItemId)
    }

    private fun toggleMealItem(mealItemType: MealItemType, mealItemId: Long) {
        var ingredient: Ingredient? = null
        var recipe: Recipe? = null
        when (mealItemType) {
            MealItemType.INGREDIENT -> {
                ingredient = ingredientsViewModel.getIngredientById(mealItemId)
            }
            MealItemType.RECIPE -> {
                recipe = recipesViewModel.getRecipeById(mealItemId)
            }
        }
        addMealItemsViewModel.apply {
            toggleItemClicked(
                createMealItemToAdd(mealItemId, ingredient, recipe)
            )
        }
    }

    private fun isAddingMealItems() = addMealItemsViewModel.isAddingMealItems

    private fun openDiscardAddingAlert() {
        var isDismissedByButton = false

        val alertDialog = HelperFunctions.createDiscardAlertDialog(
            requireContext(),
            getString(R.string.title_discard_adding_meal_items),
            onCancel = {
                isDismissedByButton = true
                setBackPressedCallback()
            },
            onConfirm = {
                isDismissedByButton = true
                handlePositiveAction()
            }
        )

        alertDialog.setOnDismissListener {
            if (!isDismissedByButton) {
                setBackPressedCallback()
            }
        }

        alertDialog.show()
    }

    private fun handlePositiveAction() {
        addMealItemsViewModel.resetAdding()
        requireMainActivity().resetSearchFragmentTopLevelDestination()
        searchViewModel.setSelectedTabPositionToStart()

        findNavController().popBackStack(R.id.detailMealPlanEditFragment, false)
    }

    override fun onResume() {
        super.onResume()
        restorePreviewAfterRotation()
    }

    private fun restorePreviewAfterRotation() {
        val isSelectedItemsNotEmpty = addMealItemsViewModel.selectedMealItems.value!!.isNotEmpty()
        if (isSelectedItemsNotEmpty) {
            binding.fragmentSearchAddLayout.show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        _addMealItemsAdapter = null
        _addMealItemsBottomSheet = null
        super.onDestroyView()
    }
}