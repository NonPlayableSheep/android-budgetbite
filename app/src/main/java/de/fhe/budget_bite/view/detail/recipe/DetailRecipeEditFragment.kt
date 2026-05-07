package de.fhe.budget_bite.view.detail.recipe

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.FragmentDetailRecipeEditBinding
import de.fhe.budget_bite.databinding.ListItemRecipeIngredientEditBinding
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.recipe.RecipeIngredient
import de.fhe.budget_bite.storage.entity.recipe.RecipeWithRecipeIngredients
import de.fhe.budget_bite.utils.enums.ChangeStatus
import de.fhe.budget_bite.utils.helper.HelperFunctions
import de.fhe.budget_bite.utils.helper.HelperFunctions.setNutritionInfoView
import de.fhe.budget_bite.utils.helper.HelperFunctions.showToast
import de.fhe.budget_bite.utils.requireMainActivity
import de.fhe.budget_bite.view.search.ingredient.IngredientsViewModel

@AndroidEntryPoint
class DetailRecipeEditFragment : Fragment(R.layout.fragment_detail_recipe_edit) {
    private var _binding: FragmentDetailRecipeEditBinding? = null
    private val binding get() = _binding!!
    private val detailRecipeViewModel: DetailRecipeViewModel by activityViewModels()
    private val ingredientsViewModel: IngredientsViewModel by activityViewModels()
    private val onIdentifierChangedWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            val input = s?.toString() ?: ""
            detailRecipeViewModel.setRecipeIdentifier(input)
        }

    }
    private fun createItemQuantityChangedWatcher(itemId: Long): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val input = s?.toString()
                val currentQuantity = input?.toIntOrNull() ?: 0

                detailRecipeViewModel.updateIngredientQuantityInRecipe(itemId, currentQuantity)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailRecipeEditBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().showActionSaveWithCallback { saveAction() }
        setBackPressedCallback()

        // observe data, set view
        detailRecipeViewModel.editRecipeWithRecipeIngredientsLiveData.observe(viewLifecycleOwner) {
            setRecipeAndIngredientViews(it)
        }

        detailRecipeViewModel.editRecipeNutritionInfo.observe(viewLifecycleOwner) {
            setRecipeNutritionInfoView(it)
        }

        binding.fragmentDetailRecipeEditAddIngredientsFab.setOnClickListener {
            detailRecipeViewModel.isAddingIngredients()
            requireMainActivity().clearFragmentBackPressedCallback()

            ingredientsViewModel.getIngredientsNotInRecipe(
                detailRecipeViewModel.getIdsOfActiveIngredients()
            )

            findNavController().navigate(R.id.action_detailRecipeEditFragment_to_ingredientsFragment)
        }
    }

    private fun saveAction() {
        if (validateFields()) {
            val isCreatingRecipe = detailRecipeViewModel.isCreatingRecipe
            detailRecipeViewModel.persistRecipeWithIngredients()
            if (isCreatingRecipe) {
                findNavController().apply {
                    popBackStack(R.id.searchFragment, false)
                    navigate(R.id.detailRecipeFragment)
                }
            } else {
                findNavController().popBackStack(R.id.detailRecipeFragment, false)
            }
            requireMainActivity().clearFragmentBackPressedCallback()
            showToast(requireContext(), getString(R.string.toast_saved))
        }
    }

    private fun setBackPressedCallback() {
        requireMainActivity().setFragmentBackPressedCallback {
            if (detailRecipeViewModel.isRecipeChanged) {
                openDiscardChangesAlert()
                false
            } else {
                true
            }
        }
    }

    private fun setRecipeAndIngredientViews(recipeWithIngredients: RecipeWithRecipeIngredients) {
        val recipe = recipeWithIngredients.recipe

        // set recipe view
        binding.apply {
            fragmentDetailRecipeEditIdentifier.setText(recipe.identifier)
            fragmentDetailRecipeEditIdentifier.addTextChangedListener(onIdentifierChangedWatcher)
        }

        // generate & set active ingredient views, use tag for itemPos
        val activeIngredients = recipeWithIngredients.ingredients.filter { it.changeStatus != ChangeStatus.DELETED }
        createIngredientEditViews(activeIngredients)
    }

    private fun createIngredientEditViews(activeRecipeIngredients: List<RecipeIngredient>) {
        val ingredientsAnchorViewIndex = binding.let {
            it.fragmentDetailRecipeEditLinearLayout.indexOfChild(
                it.fragmentDetailRecipeEditIngredientsTv
            )
        }
        var ingredientsViewIndexAddition = 1

        for (recipeIngredient in activeRecipeIngredients) {
            val ingredientViewIndex = ingredientsAnchorViewIndex + ingredientsViewIndexAddition

            val recipeIngredientEditView = createIngredientEditView(recipeIngredient, ingredientsAnchorViewIndex)
            binding.fragmentDetailRecipeEditLinearLayout.addView(recipeIngredientEditView, ingredientViewIndex)

            ingredientsViewIndexAddition++
        }
    }

    private fun createIngredientEditView(recipeIngredient: RecipeIngredient, ingredientsAnchorViewIndex: Int): View {
        val inflater = LayoutInflater.from(requireContext())
        val binding = ListItemRecipeIngredientEditBinding.inflate(inflater, binding.fragmentDetailRecipeEditLinearLayout, false)

        val ingredientId = recipeIngredient.ingredient.id

        binding.apply {
            listItemRecipeIngredientEditDeleteBtn.setOnClickListener {
                deleteIngredientFromRecipe(ingredientId, ingredientsAnchorViewIndex)
            }
            listItemRecipeIngredientEditIdentifier.text = recipeIngredient.ingredient.identifier

            listItemRecipeIngredientEditQuantity.setText(recipeIngredient.quantity.toString())
            listItemRecipeIngredientEditQuantity.addTextChangedListener(createItemQuantityChangedWatcher(
                ingredientId
            ))

            listItemRecipeIngredientEditMeasurementType.text = getString(recipeIngredient.ingredient.measurementType.displayString)
        }

        return binding.root
    }

    private fun deleteIngredientFromRecipe(ingredientId: Long, ingredientsAnchorIndex: Int) {
        // adjust sequence index in tag of following ingredient views
        val ingredientIndex = detailRecipeViewModel.getIngredientIndexFromActiveIngredients(ingredientId)
        val ingredientViewIndex = ingredientIndex + (ingredientsAnchorIndex + 1)

        binding.fragmentDetailRecipeEditLinearLayout.removeViewAt(ingredientViewIndex)

        // remove in VM
        detailRecipeViewModel.deleteIngredientFromRecipeById(ingredientId)
    }

    private fun setRecipeNutritionInfoView(nutritionInfo: EmbeddedNutritionInfo) {
        binding.apply {
            setNutritionInfoView(
                requireContext(),
                fragmentDetailRecipeEditCaloriesPrice,
                fragmentDetailRecipeEditNutritionInfo,
                nutritionInfo
            )
        }
    }

    private fun openDiscardChangesAlert() {
        var isDismissedByButton = false

        val alertDialog = HelperFunctions.createDiscardAlertDialog(
            requireContext(),
            getString(R.string.title_discard_changes),
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
        val actionId = if (detailRecipeViewModel.isCreatingRecipe) {
            R.id.searchFragment
        } else {
            R.id.detailRecipeFragment
        }

        findNavController().popBackStack(actionId, false)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        binding.apply {
            val editText = fragmentDetailRecipeEditIdentifier
            val inputLayout = editText.parent?.parent as TextInputLayout

            if (editText.text.isNullOrBlank()) {
                inputLayout.error = getString(R.string.error_field_required)
                isValid = false
            } else {
                inputLayout.error = null
            }
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireMainActivity().hideActionSave()
    }
}