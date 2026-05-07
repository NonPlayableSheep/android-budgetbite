package de.fhe.budget_bite.view.detail.ingredient

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
import de.fhe.budget_bite.databinding.FragmentDetailIngredientBinding
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.utils.helper.HelperFunctions.setNutritionInfoView

@AndroidEntryPoint
class DetailIngredientFragment : Fragment(R.layout.fragment_detail_ingredient) {
    private var _binding: FragmentDetailIngredientBinding? = null
    private val binding get() = _binding!!
    private val detailIngredientViewModel: DetailIngredientViewModel by activityViewModels()

    private val fabFromBottom: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.fab_from_bottom) }
    private val fabToBottom: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.fab_to_bottom) }
    private var isFabClicked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailIngredientBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            fragmentDetailIngredientFab.setOnClickListener {
                onFabClicked()
            }
            fragmentDetailIngredientFabEdit.setOnClickListener {
                findNavController().navigate(R.id.action_detailIngredientFragment_to_detailIngredientEditFragment)
            }
            fragmentDetailIngredientFabDelete.setOnClickListener {
                detailIngredientViewModel.deleteIngredient()
                findNavController().popBackStack(R.id.searchFragment, false)
            }
            detailIngredientViewModel.ingredientLiveData.observe(viewLifecycleOwner) {
                detailIngredientViewModel.updateEditData()
                setView(it)
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
                fragmentDetailIngredientFabEdit.show()
                fragmentDetailIngredientFabDelete.show()
            } else {
                fragmentDetailIngredientFabEdit.hide()
                fragmentDetailIngredientFabDelete.hide()
            }
        }
    }

    private fun setFabAnimation(isFabClicked: Boolean) {
        binding.apply {
            if (!isFabClicked) {
                fragmentDetailIngredientFabEdit.startAnimation(fabFromBottom)
                fragmentDetailIngredientFabDelete.startAnimation(fabFromBottom)
            } else {
                fragmentDetailIngredientFabEdit.startAnimation(fabToBottom)
                fragmentDetailIngredientFabDelete.startAnimation(fabToBottom)
            }
        }
    }

    private fun setView(ingredient: Ingredient) {
        binding.apply {
            fragmentDetailIngredientIdentifier.text = ingredient.identifier

            fragmentDetailIngredientMeasurement.text = ingredient.let {
                val measurementTypeString = getString(it.measurementType.displayString)
                val pluralizedMeasurementType = if (measurementTypeString.endsWith("s", ignoreCase = true)) {
                    measurementTypeString
                } else {
                    "${measurementTypeString}s"
                }
                "${it.measurementValueEntered} $pluralizedMeasurementType"
            }

            setNutritionInfoView(
                requireContext(),
                fragmentDetailIngredientCaloriesPrice,
                fragmentDetailIngredientNutritionInfo,
                ingredient.embeddedNutritionInfo
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isFabClicked = false
    }
}