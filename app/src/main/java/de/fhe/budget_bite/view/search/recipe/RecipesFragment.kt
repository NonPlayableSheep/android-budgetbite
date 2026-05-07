package de.fhe.budget_bite.view.search.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.FragmentRecipesBinding
import de.fhe.budget_bite.view.detail.recipe.DetailRecipeViewModel
import de.fhe.budget_bite.view.search.add_meal_items.AddMealItemsViewModel

@AndroidEntryPoint
class RecipesFragment : Fragment(R.layout.fragment_recipes) {
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private var _adapter: RecipesAdapter? = null
    private val adapter get() = _adapter!!
    private val recipesViewModel: RecipesViewModel by activityViewModels()
    private val detailRecipeViewModel: DetailRecipeViewModel by activityViewModels()
    private val addMealItemsViewModel: AddMealItemsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipesBinding.inflate(inflater)

        _adapter = RecipesAdapter(
            ::onItemClick,
            addMealItemsViewModel.isAddingMealItems,
            addMealItemsViewModel::isMealItemSelected
        )
        binding.fragmentRecipesRv.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recipesViewModel.recipesLiveData.observe(viewLifecycleOwner) {
            adapter.updateDataset(it)
        }

        binding.apply {
            if (addMealItemsViewModel.isAddingMealItems) {
                fragmentRecipesCreateBtn.hide()

                addMealItemsViewModel.selectedMealItems.observe(viewLifecycleOwner) {
                    adapter.notifyDataSetChanged()
                }
            } else {
                fragmentRecipesCreateBtn.setOnClickListener {
                    detailRecipeViewModel.createRecipe()
                    findNavController().navigate(R.id.action_searchFragment_to_detailRecipeEditFragment)
                }
            }
        }
    }

    private fun onItemClick(mealItemId: Long) {
        if (addMealItemsViewModel.isAddingMealItems) {
            toggleMealItem(mealItemId)
        } else {
            val clickedRecipe = recipesViewModel.getRecipeById(mealItemId)
            detailRecipeViewModel.loadRecipeById(clickedRecipe.id)
            findNavController().navigate(R.id.action_searchFragment_to_detailRecipeFragment)
        }
    }

    private fun toggleMealItem(mealItemId: Long) {
        val recipe = recipesViewModel.getRecipeById(mealItemId)
        addMealItemsViewModel.apply {
            toggleItemClicked(
                createMealItemToAdd(
                    mealItemId,
                    recipe = recipe
                )
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        _adapter = null
        super.onDestroyView()
    }
}