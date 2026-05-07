package de.fhe.budget_bite.view.search.ingredient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.FragmentIngredientsBinding
import de.fhe.budget_bite.utils.ClickTrace
import de.fhe.budget_bite.utils.gone
import de.fhe.budget_bite.utils.helper.HelperFunctions
import de.fhe.budget_bite.utils.requireMainActivity
import de.fhe.budget_bite.utils.show
import de.fhe.budget_bite.view.detail.ingredient.DetailIngredientViewModel
import de.fhe.budget_bite.view.detail.recipe.DetailRecipeViewModel
import de.fhe.budget_bite.view.search.add_meal_items.AddMealItemsViewModel

@AndroidEntryPoint
class IngredientsFragment : Fragment(R.layout.fragment_ingredients) {
    private var _binding: FragmentIngredientsBinding? = null
    private val binding get() = _binding!!
    private var _adapter: IngredientsAdapter? = null
    private val adapter get() = _adapter!!
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var _bottomSheetAdapter: IngredientsAdapter? = null
    private val bottomSheetAdapter get() = _bottomSheetAdapter!!

    private val ingredientsViewModel: IngredientsViewModel by activityViewModels()
    private val detailIngredientViewModel: DetailIngredientViewModel by activityViewModels()
    private val detailRecipeViewModel: DetailRecipeViewModel by activityViewModels()
    private val addMealItemsViewModel: AddMealItemsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIngredientsBinding.inflate(inflater)

        _adapter = IngredientsAdapter(
            ::onItemClick,
            ::isIngredientClicked,
            detailRecipeViewModel.isAddingIngredients,
            false,
            addMealItemsViewModel.isAddingMealItems,
            addMealItemsViewModel::isMealItemSelected
        )
        binding.fragmentIngredientsRv.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ingredientsViewModel.ingredientsLiveData.observe(viewLifecycleOwner) {
            adapter.updateDataset(it)
        }

        binding.apply {
            val isAddingIngredients = detailRecipeViewModel.isAddingIngredients
            val isAddingMealItems = addMealItemsViewModel.isAddingMealItems
            if (isAddingIngredients) {
                setIsAddingIngredients()
            }
            if (!isAddingIngredients && !isAddingMealItems) {
                showCreateBtn()
            }
            if (isAddingMealItems) {
                addMealItemsViewModel.selectedMealItems.observe(viewLifecycleOwner) {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setIsAddingIngredients() {
        binding.apply {
            fragmentIngredientsAddIngredientsPreview.setOnClickListener {
                showAddedIngredientsBottomSheet()
            }

            fragmentIngredientsAddIngredientsAddBtn.setOnClickListener {
                val selectedIngredients = ingredientsViewModel.getClickedIngredients()
                detailRecipeViewModel.addIngredientsToRecipe(selectedIngredients)

                ingredientsViewModel.clearClickedIngredients()
                requireMainActivity().clearFragmentBackPressedCallback()
                resetAddIngredientsToRecipe()

                findNavController().popBackStack(R.id.detailRecipeEditFragment, false)
            }

            ingredientsViewModel.clickedIngredientIds.observe(viewLifecycleOwner) {
                val count = it.size
                fragmentIngredientsAddIngredientsCount.text = count.toString()

                if (ingredientsViewModel.isFirstIngredientAdded) {
                    fragmentIngredientsAddIngredientsLayout.show()
                }
                if (it.isEmpty()) {
                    fragmentIngredientsAddIngredientsLayout.gone()
                }
            }

            setBackPressedCallback()
        }
    }

    private fun setBackPressedCallback() {
        requireMainActivity().setFragmentBackPressedCallback {
            val clickedIngredients = ingredientsViewModel.clickedIngredientIds.value!!
            if (clickedIngredients.isNotEmpty()) {
                openDiscardAddingAlert()
                false
            } else {
                detailRecipeViewModel.resetIsAddingIngredients()
                ingredientsViewModel.clearClickedIngredients()
                resetAddIngredientsToRecipe()
                true
            }
        }
    }

    private fun showCreateBtn() {
        binding.apply {
            fragmentIngredientsCreateBtn.show()
            fragmentIngredientsCreateBtn.setOnClickListener {
                ClickTrace.onClickPerformed()
                detailIngredientViewModel.createIngredient()
                findNavController().navigate(R.id.action_searchFragment_to_detailIngredientEditFragment)
            }
        }
    }

    private fun onItemClick(position: Int, ingredientId: Long, isFromDialog: Boolean = false) {
        if (!detailRecipeViewModel.isAddingIngredients && !addMealItemsViewModel.isAddingMealItems) {
            val clickedIngredient = ingredientsViewModel.ingredientsLiveData.value!![position]
            detailIngredientViewModel.loadIngredientById(clickedIngredient.id)
            findNavController().navigate(R.id.action_searchFragment_to_detailIngredientFragment)
        } else if (detailRecipeViewModel.isAddingIngredients) {
            toggleIngredientClicked(position, ingredientId, isFromDialog)
        } else {
            toggleMealItem(ingredientId)
        }
    }

    private fun toggleIngredientClicked(position: Int, ingredientId: Long, isFromDialog: Boolean) {
        // Toggle im ViewModel, um den Status zu ändern
        ingredientsViewModel.toggleIngredientClicked(ingredientId)

        val ingredientPos = if (isFromDialog) {
            bottomSheetAdapter.notifyItemChanged(position)

            val ingredient = ingredientsViewModel.ingredientsLiveData.value!!.find { it.id == ingredientId }
            ingredientsViewModel.ingredientsLiveData.value!!.indexOf(ingredient)
        } else {
            position
        }

        adapter.notifyItemChanged(ingredientPos)
    }

    private fun isIngredientClicked(ingredientId: Long) = ingredientsViewModel.isIngredientClicked(ingredientId)

    private fun showAddedIngredientsBottomSheet() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bottom_sheet_add_ingredients, null)
        bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(dialogView)
        val bottomSheetRecyclerView = dialogView.findViewById<RecyclerView>(R.id.dialog_bottom_sheet_add_ingredients_rv)
        _bottomSheetAdapter = IngredientsAdapter(
            ::onItemClick,
            ::isIngredientClicked,
            detailRecipeViewModel.isAddingIngredients,
            true,
            addMealItemsViewModel.isAddingMealItems,
            addMealItemsViewModel::isMealItemSelected,
            ingredientsViewModel.getClickedIngredients().toMutableList()
        )
        bottomSheetRecyclerView.adapter = bottomSheetAdapter
        bottomSheetDialog.show()
    }

    private fun toggleMealItem(mealItemId: Long) {
        val ingredient = ingredientsViewModel.getIngredientById(mealItemId)
        addMealItemsViewModel.apply {
            toggleItemClicked(
                createMealItemToAdd(
                    mealItemId,
                    ingredient = ingredient
                )
            )
        }
    }

    private fun openDiscardAddingAlert() {
        var isDismissedByButton = false

        val alertDialog = HelperFunctions.createDiscardAlertDialog(
            requireContext(),
            getString(R.string.title_discard_adding_ingredients),
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
        detailRecipeViewModel.resetIsAddingIngredients()
        ingredientsViewModel.clearClickedIngredients()
        resetAddIngredientsToRecipe()
        findNavController().popBackStack(R.id.detailRecipeEditFragment, false)
    }

    private fun resetAddIngredientsToRecipe() = ingredientsViewModel.getAllIngredients()

    override fun onResume() {
        super.onResume()
        restoreAddIngredientsPreview()
    }

    private fun restoreAddIngredientsPreview() {
        val isClickedIngredientsNotEmpty = ingredientsViewModel.clickedIngredientIds.value!!.isNotEmpty()
        if (isClickedIngredientsNotEmpty) {
            binding.fragmentIngredientsAddIngredientsLayout.show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        _adapter = null
        _bottomSheetAdapter = null
        super.onDestroyView()
    }
}