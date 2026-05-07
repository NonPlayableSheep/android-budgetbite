package de.fhe.budget_bite.view.detail.meal_plan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.ChildFragmentMealItemEditBinding
import de.fhe.budget_bite.databinding.ChildFragmentMealTypeBinding
import de.fhe.budget_bite.databinding.FragmentDetailMealPlanEditBinding
import de.fhe.budget_bite.storage.entity.EmbeddedNutritionInfo
import de.fhe.budget_bite.storage.entity.meal_plan.MealItemWithDetails
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlan
import de.fhe.budget_bite.storage.entity.meal_plan.MealPlanWithItems
import de.fhe.budget_bite.utils.enums.MealItemType
import de.fhe.budget_bite.utils.enums.MealType
import de.fhe.budget_bite.utils.enums.MeasurementType
import de.fhe.budget_bite.utils.helper.HelperFunctions
import de.fhe.budget_bite.utils.helper.HelperFunctions.setCaloriesPriceView
import de.fhe.budget_bite.utils.helper.HelperFunctions.setNutritionInfoView
import de.fhe.budget_bite.utils.helper.HelperFunctions.showToast
import de.fhe.budget_bite.utils.requireMainActivity
import de.fhe.budget_bite.utils.show
import de.fhe.budget_bite.view.search.add_meal_items.AddMealItemsViewModel

@AndroidEntryPoint
class DetailMealPlanEditFragment : Fragment(R.layout.fragment_detail_meal_plan_edit) {
    private var _binding: FragmentDetailMealPlanEditBinding? = null
    private val binding get() = _binding!!
    private val detailMealPlanViewModel: DetailMealPlanViewModel by activityViewModels()
    private val addMealItemsViewModel: AddMealItemsViewModel by activityViewModels()
    private fun createItemQuantityChangedWatcher(itemId: Long, itemMealType: MealType): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val input = s?.toString()
                val currentQuantity = input?.toIntOrNull() ?: 0

                detailMealPlanViewModel.updateMealItemQuantityInMealPlan(itemId, itemMealType, currentQuantity)
            }
        }
    }
    private val onIdentifierChangedWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            val input = s?.toString() ?: ""
            detailMealPlanViewModel.setMealPlanIdentifier(input)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailMealPlanEditBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMealTypeItemViews()

        requireMainActivity().showActionSaveWithCallback { saveAction() }
        setBackPressedCallback()

        detailMealPlanViewModel.editMealPlanLiveData.observe(viewLifecycleOwner) {
            handleDataUpdate(it)
        }

        detailMealPlanViewModel.editNutritionInfo.observe(viewLifecycleOwner) {
            setMealPlanNutritionInfoView(it)
        }
    }

    private fun saveAction() {
        if (validateFields()) {
            val isCreatingMealPlan = detailMealPlanViewModel.isCreatingMealPlan
            detailMealPlanViewModel.persistMealPlan()
            if (isCreatingMealPlan) {
                findNavController().apply {
                    popBackStack(R.id.searchFragment, false)
                    navigate(R.id.detailMealPlanFragment)
                }
            } else {
                findNavController().popBackStack(R.id.detailMealPlanFragment, false)
            }
            requireMainActivity().clearFragmentBackPressedCallback()
            showToast(requireContext(), getString(R.string.toast_saved))
        }
    }

    private fun setBackPressedCallback() {
        requireMainActivity().setFragmentBackPressedCallback {
            if (detailMealPlanViewModel.isMealPlanChanged) {
                openDiscardChangesAlert()
                false
            } else {
                true
            }
        }
    }

    private fun handleDataUpdate(mealPlanWithItems: MealPlanWithItems) {
        val updatedType = detailMealPlanViewModel.getUpdatedMealType()
        val updatedItemId = detailMealPlanViewModel.getUpdatedMealItemId()
        if (updatedType != null || updatedItemId != null) {
            updatedType?.let {
                updateMealTypeNutritionInfo(
                    it,
                    mealPlanWithItems.infoByMealType[it]!!
                )
            }
            updatedItemId?.let {
                updateItemNutritionInfo(it)
            }
            detailMealPlanViewModel.resetUpdateTracking()
        }
    }

    private fun initMealTypeItemViews() {
        val mealPlanWithItems = detailMealPlanViewModel.editMealPlanLiveData.value!!
        mealPlanWithItems.let {
            setMealPlanView(it.mealPlan)
            createMealTypeEditViews(it.infoByMealType)
            createMealItemEditViews(
                detailMealPlanViewModel.getActiveItemsByType()
            )
        }
    }

    private fun setMealPlanView(mealPlan: MealPlan) {
        binding.apply {
            fragmentDetailMealPlanEditIdentifier.setText(mealPlan.identifier)
            fragmentDetailMealPlanEditIdentifier.addTextChangedListener(onIdentifierChangedWatcher)

            setMealPlanNutritionInfoView(mealPlan.embeddedNutritionInfoInTotal)
        }
    }

    private fun setMealPlanNutritionInfoView(nutritionInfo: EmbeddedNutritionInfo) {
        binding.apply {
            setNutritionInfoView(
                requireContext(),
                fragmentDetailMealPlanEditCaloriesPrice,
                fragmentDetailMealPlanEditNutritionInfo,
                nutritionInfo
            )
        }
    }

    private fun createMealTypeEditViews(nutritionInfoByType: Map<MealType, EmbeddedNutritionInfo>) {
        binding.apply {
            val anchorIndex = fragmentDetailMealPlanEditLayout.indexOfChild(
                fragmentDetailMealPlanEditAnchor
            )
            var addIndexIncrement = 1
            for (mealType in nutritionInfoByType.keys) {
                val mealTypeView = createMealTypeEditView(mealType, nutritionInfoByType[mealType]!!)
                fragmentDetailMealPlanEditLayout.addView(mealTypeView, anchorIndex + addIndexIncrement)
                addIndexIncrement++
            }
        }
    }

    private fun createMealTypeEditView(mealType: MealType, nutritionInfo: EmbeddedNutritionInfo): View {
        val inflater = LayoutInflater.from(requireContext())
        val binding = ChildFragmentMealTypeBinding.inflate(inflater, binding.fragmentDetailMealPlanEditLayout, false)

        binding.apply {
            childFragmentMealPlanMealTypeIdentifier.text = mealType.toString()
            setCaloriesPriceView(
                requireContext(),
                childFragmentMealPlanMealTypeCalories,
                childFragmentMealPlanMealTypePrice,
                nutritionInfo.calories,
                nutritionInfo.price
            )
            childFragmentMealTypeEditAddBtn.show()
            childFragmentMealTypeEditAddBtn.setOnClickListener {
                requireMainActivity().apply {
                    clearFragmentBackPressedCallback()
                    setSearchFragmentSecondLevelDestination()
                }
                addMealItemsViewModel.isAddingMealItems(mealType)
                findNavController().navigate(R.id.action_detailMealPlanEditFragment_to_searchFragment)
            }
        }
        binding.root.tag = mealType
        return binding.root
    }

    private fun updateMealTypeNutritionInfo(mealType: MealType, nutritionUpdate: EmbeddedNutritionInfo) {
        val mealTypeView = binding.fragmentDetailMealPlanEditLayout.findViewWithTag<ViewGroup>(mealType)
        val caloriesTextView = mealTypeView.findViewById<TextView>(R.id.child_fragment_meal_plan_meal_type_calories)
        val priceTextView = mealTypeView.findViewById<TextView>(R.id.child_fragment_meal_plan_meal_type_price)

        setCaloriesPriceView(
            requireContext(),
            caloriesTextView,
            priceTextView,
            nutritionUpdate.calories,
            nutritionUpdate.price
        )
    }

    private fun createMealItemEditViews(itemsByType: Map<MealType, List<MealItemWithDetails>>) {
        binding.apply {
            for (mealType in itemsByType.keys) {
                var addIndexIncrement = 1
                val mealTypeView = fragmentDetailMealPlanEditLayout.findViewWithTag<View>(mealType)
                val mealTypeIndex = fragmentDetailMealPlanEditLayout.indexOfChild(mealTypeView)

                for (mealItem in itemsByType[mealType]!!) {
                    val itemView = createMealItemEditView(mealItem)
                    fragmentDetailMealPlanEditLayout.addView(itemView, mealTypeIndex + addIndexIncrement)
                    addIndexIncrement++
                }
            }
        }
    }

    private fun createMealItemEditView(mealItem: MealItemWithDetails): View {
        val inflater = LayoutInflater.from(requireContext())
        val binding = ChildFragmentMealItemEditBinding.inflate(inflater, binding.fragmentDetailMealPlanEditLayout, false)

        binding.apply {
            val quantity = mealItem.quantity
            val calories: Float
            val price: Float
            var nutritionInfo: EmbeddedNutritionInfo = EmbeddedNutritionInfo()
            var identifier: String = ""
            var measurementType: MeasurementType = MeasurementType.WEIGHT
            // due to multiple LiveData updates with initially incomplete data before final render, coming from the trigger in DetailMealPlan
            when (mealItem.mealItemType) {
                MealItemType.INGREDIENT -> {
                    mealItem.ingredient?.let {
                        nutritionInfo = it.normalisedEmbeddedNutritionInfo
                        identifier = it.identifier
                        measurementType = it.measurementType
                    }
                }
                MealItemType.RECIPE -> {
                    mealItem.recipe?.let {
                        nutritionInfo = it.embeddedNutritionInfoInTotal
                        identifier = it.identifier
                        measurementType = MeasurementType.QUANTITY
                    }
                }
            }
            calories = nutritionInfo.calories * quantity
            price = nutritionInfo.price * quantity

            setCaloriesPriceView(
                requireContext(),
                childFragmentMealItemEditCalories,
                childFragmentMealItemEditPrice,
                calories,
                price
            )
            childFragmentMealItemEditIdentifier.text = identifier
            childFragmentMealItemEditQuantity.setText(quantity.toString())
            childFragmentMealItemEditQuantity.addTextChangedListener(createItemQuantityChangedWatcher(
                mealItem.id,
                mealItem.mealType
            ))
            childFragmentMealItemEditMeasurementType.text = getString(measurementType.displayString)
            childFragmentMealItemEditDeleteBtn.setOnClickListener {
                deleteMealItemFromMealPlan(mealItem.id, mealItem.mealType)
            }
        }
        binding.root.tag = mealItem.id
        return binding.root
    }

    private fun updateItemNutritionInfo(mealItemId: Long) {
        val mealItemView = binding.fragmentDetailMealPlanEditLayout.findViewWithTag<ViewGroup>(mealItemId)
        val caloriesTextView = mealItemView.findViewById<TextView>(R.id.child_fragment_meal_item_edit_calories)
        val priceTextView = mealItemView.findViewById<TextView>(R.id.child_fragment_meal_item_edit_price)
        val quantity = detailMealPlanViewModel.getUpdatedMealItemQuantity()
        val nutritionInfo = detailMealPlanViewModel.getUpdatedMealItemNutritionInfo()

        val calories = nutritionInfo.calories * quantity
        val price = nutritionInfo.price * quantity
        setCaloriesPriceView(
            requireContext(),
            caloriesTextView,
            priceTextView,
            calories,
            price
        )
    }

    private fun deleteMealItemFromMealPlan(mealItemId: Long, mealType: MealType) {
        binding.apply {
            // get view with tag ID
            val itemView = fragmentDetailMealPlanEditLayout.findViewWithTag<View>(mealItemId)
            fragmentDetailMealPlanEditLayout.removeView(itemView)

            detailMealPlanViewModel.deleteMealItemFromMealPlanById(mealItemId, mealType)
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
        val actionId = if (detailMealPlanViewModel.isCreatingMealPlan) {
            R.id.searchFragment
        } else {
            R.id.detailMealPlanFragment
        }

        findNavController().popBackStack(actionId, false)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        binding.apply {
            val editText = fragmentDetailMealPlanEditIdentifier
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