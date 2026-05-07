package de.fhe.budget_bite.view.detail.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.FragmentDetailRecipeBinding
import de.fhe.budget_bite.databinding.ListItemRecipeIngredientBinding
import de.fhe.budget_bite.storage.entity.recipe.Recipe
import de.fhe.budget_bite.storage.entity.recipe.RecipeIngredient
import de.fhe.budget_bite.utils.helper.HelperFunctions

@AndroidEntryPoint
class DetailRecipeFragment : Fragment(R.layout.fragment_detail_recipe) {
    private var _binding: FragmentDetailRecipeBinding? = null
    private val binding get() = _binding!!
    private val detailRecipeViewModel: DetailRecipeViewModel by activityViewModels()

    private val fabFromBottom: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.fab_from_bottom) }
    private val fabToBottom: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.fab_to_bottom) }
    private var isFabClicked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailRecipeBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailRecipeViewModel.recipeWithIngredientsLiveData.observe(viewLifecycleOwner) {
            clearDynamicViews()
            createIngredientViews(it.ingredients)
            setRecipeInfoInView(it.recipe)
            detailRecipeViewModel.updateEditData()
        }

        binding.apply {
            fragmentDetailRecipeFab.setOnClickListener {
                onFabClicked()
            }
            fragmentDetailRecipeFabEdit.setOnClickListener {
                findNavController().navigate(R.id.action_detailRecipeFragment_to_detailRecipeEditFragment)
            }
            fragmentDetailRecipeFabDelete.setOnClickListener {
                detailRecipeViewModel.deleteRecipeWithIngredients()
                findNavController().popBackStack(R.id.searchFragment, false)
            }
        }
    }

    private fun onFabClicked() {
        setFabVisibility(isFabClicked)
        setFabAnimation(isFabClicked)
        isFabClicked = !isFabClicked
    }

    private fun setFabVisibility(isFabClicked: Boolean) {
        binding.apply {
            if (!isFabClicked) {
                fragmentDetailRecipeFabEdit.show()
                fragmentDetailRecipeFabDelete.show()
            } else {
                fragmentDetailRecipeFabEdit.hide()
                fragmentDetailRecipeFabDelete.hide()
            }
        }
    }

    private fun setFabAnimation(isFabClicked: Boolean) {
        binding.apply {
            if (!isFabClicked) {
                fragmentDetailRecipeFabEdit.startAnimation(fabFromBottom)
                fragmentDetailRecipeFabDelete.startAnimation(fabFromBottom)
            } else {
                fragmentDetailRecipeFabEdit.startAnimation(fabToBottom)
                fragmentDetailRecipeFabDelete.startAnimation(fabToBottom)
            }
        }
    }

    private fun setRecipeInfoInView(recipe: Recipe) {
        binding.apply {
            fragmentDetailRecipeIdentifier.text = recipe.identifier
            HelperFunctions.setNutritionInfoView(
                requireContext(),
                fragmentDetailRecipeCaloriesPrice,
                fragmentDetailRecipeNutritionInfo,
                recipe.embeddedNutritionInfoInTotal
            )
        }
    }

    private fun clearDynamicViews() {
        val targetIndex = binding.let {
            it.fragmentDetailRecipeLinearLayout.indexOfChild(
                it.fragmentDetailRecipeIngredientsTv
            )
        }
        if (targetIndex != -1) {
            // Remove all views after the targetIndex
            binding.fragmentDetailRecipeLinearLayout.removeViewsInLayout(targetIndex + 1,
                binding.fragmentDetailRecipeLinearLayout.childCount - (targetIndex + 1))
        }
    }

    private fun createIngredientViews(recipeIngredients: List<RecipeIngredient>) {
        val targetIndex = binding.let {
            it.fragmentDetailRecipeLinearLayout.indexOfChild(
                it.fragmentDetailRecipeIngredientsTv
            )
        }
        var targetIndexAddition = 1

        for (recipeIngredient in recipeIngredients) {
            val recipeIngredientView = createIngredientView(recipeIngredient)
            binding.fragmentDetailRecipeLinearLayout.addView(recipeIngredientView, targetIndex + targetIndexAddition)
            targetIndexAddition++
        }
    }

    private fun createIngredientView(recipeIngredient: RecipeIngredient): View {
        val inflater = LayoutInflater.from(requireContext())
        val binding = ListItemRecipeIngredientBinding.inflate(inflater, binding.fragmentDetailRecipeLinearLayout, false)

        binding.apply {
            listItemRecipeIngredientIdentifier.text = recipeIngredient.ingredient.identifier
            listItemRecipeIngredientQuantity.text = recipeIngredient.quantity.toString()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isFabClicked = false
    }
}