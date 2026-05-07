package de.fhe.budget_bite.view.detail.meal_plan

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
import de.fhe.budget_bite.databinding.ChildFragmentMealItemBinding
import de.fhe.budget_bite.databinding.ChildFragmentMealTypeBinding
import de.fhe.budget_bite.databinding.FragmentDetailMealPlanBinding
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemWithDetails
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlan
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType
import de.fhe.budget_bite.utils.helper.HelperFunctions.setCaloriesPriceView
import de.fhe.budget_bite.utils.helper.HelperFunctions.setNutritionInfoView

@AndroidEntryPoint
class DetailMealPlanFragment : Fragment(R.layout.fragment_detail_meal_plan) {
    private var _binding: FragmentDetailMealPlanBinding? = null
    private val binding get() = _binding!!
    private val detailMealPlanViewModel: DetailMealPlanViewModel by activityViewModels()
    private val fabFromBottom: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.fab_from_bottom) }
    private val fabToBottom: Animation by lazy { AnimationUtils.loadAnimation(requireContext(), R.anim.fab_to_bottom) }
    private var isFabClicked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailMealPlanBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailMealPlanViewModel.mealPlanLiveData.observe(viewLifecycleOwner) {
            clearDynamicViews()
            setMealInfoInView(it.mealPlan)
            createMealTypeItemViews(it.itemsByMealType, it.infoByMealType)
            detailMealPlanViewModel.updateEditData()
        }

        binding.apply {
            fragmentDetailMealPlanFab.setOnClickListener {
                onFabClicked()
            }
            fragmentDetailMealPlanFabEdit.setOnClickListener {
                findNavController().navigate(R.id.action_detailMealPlanFragment_to_detailMealPlanEditFragment)
            }
            fragmentDetailMealPlanFabDelete.setOnClickListener {
                detailMealPlanViewModel.deleteMealPlan()
                findNavController().popBackStack(R.id.searchFragment, false)
            }
        }
    }

    private fun clearDynamicViews() {
        binding.apply {
            val anchorIndex = fragmentDetailMealPlanLayout.indexOfChild(
                fragmentDetailMealPlanAnchor
            )
            if (anchorIndex != -1) {
                fragmentDetailMealPlanLayout.removeViewsInLayout(
                    anchorIndex + 1,
                    fragmentDetailMealPlanLayout.childCount - (anchorIndex + 1)
                )
            }
        }
    }

    private fun setMealInfoInView(mealPlan: MealPlan) {
        binding.apply {
            fragmentDetailMealPlanIdentifier.text = mealPlan.identifier

            setNutritionInfoView(
                requireContext(),
                fragmentDetailMealPlanCaloriesPrice,
                fragmentDetailMealPlanNutritionInfo,
                mealPlan.embeddedNutritionInfoInTotal
            )
        }
    }

    private fun createMealTypeItemViews(mealItemsByType: Map<MealType, List<MealItemWithDetails>>, nutritionInfoByType: Map<MealType, EmbeddedNutritionInfo>) {
        createMealTypeViews(nutritionInfoByType)
        createMealItemViews(mealItemsByType)
    }

    private fun createMealTypeViews(nutritionInfoByType: Map<MealType, EmbeddedNutritionInfo>) {
        binding.apply {
            val anchorIndex = fragmentDetailMealPlanLayout.indexOfChild(
                fragmentDetailMealPlanAnchor
            )
            var addIndexIncrement = 1
            for (mealType in nutritionInfoByType.keys) {
                val mealTypeView = createMealTypeView(mealType, nutritionInfoByType[mealType]!!)
                fragmentDetailMealPlanLayout.addView(mealTypeView, anchorIndex + addIndexIncrement)
                addIndexIncrement++
            }
        }
    }

    private fun createMealTypeView(mealType: MealType, nutritionInfo: EmbeddedNutritionInfo): View {
        val inflater = LayoutInflater.from(requireContext())
        val binding = ChildFragmentMealTypeBinding.inflate(inflater, binding.fragmentDetailMealPlanLayout, false)

        binding.apply {
            childFragmentMealPlanMealTypeIdentifier.text = mealType.toString()
            setCaloriesPriceView(
                requireContext(),
                childFragmentMealPlanMealTypeCalories,
                childFragmentMealPlanMealTypePrice,
                nutritionInfo.calories,
                nutritionInfo.price
            )
        }
        binding.root.tag = mealType
        return binding.root
    }

    private fun createMealItemViews(itemsByType: Map<MealType, List<MealItemWithDetails>>) {
        binding.apply {
            for (mealType in itemsByType.keys) {
                var addIndexIncrement = 1
                val mealTypeView = fragmentDetailMealPlanLayout.findViewWithTag<View>(mealType)
                val mealTypeIndex = fragmentDetailMealPlanLayout.indexOfChild(mealTypeView)

                for (mealItem in itemsByType[mealType]!!) {
                    val itemView = createMealItemView(mealItem)
                    fragmentDetailMealPlanLayout.addView(itemView, mealTypeIndex + addIndexIncrement)
                    addIndexIncrement++
                }
            }
        }
    }

    private fun createMealItemView(mealItem: MealItemWithDetails): View {
        val inflater = LayoutInflater.from(requireContext())
        val binding = ChildFragmentMealItemBinding.inflate(inflater, binding.fragmentDetailMealPlanLayout, false)

        binding.apply {
            val quantity = mealItem.quantity
            val calories: Float
            val price: Float
            var nutritionInfo: EmbeddedNutritionInfo = EmbeddedNutritionInfo()
            var identifier: String = ""
            // due to multiple LiveData updates with initially incomplete data before final render regarding newly adds before
            when (mealItem.mealItemType) {
                MealItemType.INGREDIENT -> {
                    mealItem.ingredient?.let {
                        nutritionInfo = it.normalisedEmbeddedNutritionInfo
                        identifier = it.identifier
                    }
                }
                MealItemType.RECIPE -> {
                    mealItem.recipe?.let {
                        nutritionInfo = it.embeddedNutritionInfoInTotal
                        identifier = it.identifier
                    }
                }
            }
            calories = nutritionInfo.calories * quantity
            price = nutritionInfo.price * quantity

            childFragmentMealItemQuantity.text = quantity.toString()
            setCaloriesPriceView(
                requireContext(),
                childFragmentMealItemCalories,
                childFragmentMealItemPrice,
                calories,
                price
            )
            childFragmentMealItemIdentifier.text = identifier
        }
        binding.root.tag = mealItem.id
        return binding.root
    }

    private fun onFabClicked() {
        setFabVisibility(isFabClicked)
        setFabAnimation(isFabClicked)
        isFabClicked = !isFabClicked
    }

    private fun setFabVisibility(isFabClicked: Boolean) {
        binding.apply {
            if (!isFabClicked) {
                fragmentDetailMealPlanFabEdit.show()
                fragmentDetailMealPlanFabDelete.show()
            } else {
                fragmentDetailMealPlanFabEdit.hide()
                fragmentDetailMealPlanFabDelete.hide()
            }
        }
    }

    private fun setFabAnimation(isFabClicked: Boolean) {
        binding.apply {
            if (!isFabClicked) {
                fragmentDetailMealPlanFabEdit.startAnimation(fabFromBottom)
                fragmentDetailMealPlanFabDelete.startAnimation(fabFromBottom)
            } else {
                fragmentDetailMealPlanFabEdit.startAnimation(fabToBottom)
                fragmentDetailMealPlanFabDelete.startAnimation(fabToBottom)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isFabClicked = false
    }
}