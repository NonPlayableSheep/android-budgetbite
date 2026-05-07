package de.fhe.budget_bite.view.detail.ingredient

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.FragmentDetailIngredientEditBinding
import de.fhe.budget_bite.storage.entity.Ingredient
import de.fhe.budget_bite.utils.enums.MeasurementType
import de.fhe.budget_bite.utils.helper.HelperFunctions.createDiscardAlertDialog
import de.fhe.budget_bite.utils.helper.HelperFunctions.showToast
import de.fhe.budget_bite.utils.requireMainActivity
import de.fhe.budget_bite.utils.toCalories
import de.fhe.budget_bite.utils.toOneDecimal
import de.fhe.budget_bite.utils.toPrice

@AndroidEntryPoint
class DetailIngredientEditFragment : Fragment(R.layout.fragment_detail_ingredient_edit) {
    private var _binding: FragmentDetailIngredientEditBinding? = null
    private val binding get() = _binding!!
    private val detailIngredientViewModel: DetailIngredientViewModel by activityViewModels()
    private val fieldUpdateMap: Map<TextInputEditText, (String) -> Unit> by lazy {
        mapOf(
            binding.fragmentDetailIngredientEditIdentifier to { detailIngredientViewModel.updateIdentifier(it) },
            binding.fragmentDetailIngredientEditCalories to { detailIngredientViewModel.updateCalories(it.toFloatOrNull() ?: 0.0f) },
            binding.fragmentDetailIngredientEditCarbs to { detailIngredientViewModel.updateCarbs(it.toFloatOrNull() ?: 0.0f) },
            binding.fragmentDetailIngredientEditFat to { detailIngredientViewModel.updateFat(it.toFloatOrNull() ?: 0.0f) },
            binding.fragmentDetailIngredientEditProtein to { detailIngredientViewModel.updateProtein(it.toFloatOrNull() ?: 0.0f) },
            binding.fragmentDetailIngredientEditPrice to { detailIngredientViewModel.updatePrice(it.toFloatOrNull() ?: 0.0f) }
        )
    }
    private val universalTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val editText = view?.findFocus() as? TextInputEditText ?: return
            val updateAction = fieldUpdateMap[editText] ?: return
            updateAction(s.toString())
        }
    }
    private fun createDecimalLimiter(decimalPlaces: Int): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val input = it.toString()
                    if (input.contains(".")) {
                        val parts = input.split(".")
                        if (parts.size > 1 && parts[1].length > decimalPlaces) {
                            it.replace(0, it.length, "${parts[0]}.${parts[1].take(decimalPlaces)}")
                        }
                    }
                }
            }
        }
    }
    private lateinit var measurementTypeList: List<MeasurementType>
    private val onMeasurementTypeItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        private var isInitialized = false

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (!isInitialized) {
                isInitialized = true
                return
            }

            val selectedMeasurementType = measurementTypeList[position]
            detailIngredientViewModel.updateMeasurementType(selectedMeasurementType)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    private val onMeasurementValueChangedWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (s.isNullOrBlank()) return

            binding.apply {
                val value = s.toString().toIntOrNull() ?: 1

                if (value <= 0) {
                    resetMeasurementValueToDefault()
                    showToast(requireContext(), getString(R.string.error_measurement_min))
                } else {
                    detailIngredientViewModel.updateMeasurementValue(value)
                }
            }
        }
    }
    private val nutritionInfoDecimalLimit = 1
    private val nutritionInfoPriceDecimalLimit = 2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailIngredientEditBinding.inflate(inflater)

        initMeasurementTypeSpinner()

        return binding.root
    }

    private fun initMeasurementTypeSpinner() {
        binding.apply {
            measurementTypeList = MeasurementType.entries
            val measurementTypeDisplayStringList: Array<String> =
                measurementTypeList.map {
                    requireContext().getString(it.displayString)
                }.toTypedArray()
            fragmentDetailIngredientEditSpinnerMeasurementType.apply {
                adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    measurementTypeDisplayStringList
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().showActionSaveWithCallback { saveAction() }
        setBackPressedCallback()

        detailIngredientViewModel.editIngredientLiveData.observe(viewLifecycleOwner) {
            setView(it)
            setFormWatchers()
        }
    }

    private fun saveAction() {
        if (validateFields()) {
            detailIngredientViewModel.persistIngredient()

            if (detailIngredientViewModel.isCreatingIngredient) {
                findNavController().apply {
                    popBackStack(R.id.searchFragment, false)
                    navigate(R.id.detailIngredientFragment)
                }
            } else {
                findNavController().popBackStack(R.id.detailIngredientFragment, false)
            }
            requireMainActivity().clearFragmentBackPressedCallback()
            showToast(requireContext(), getString(R.string.toast_saved))
        }
    }

    private fun setBackPressedCallback() {
        requireMainActivity().setFragmentBackPressedCallback {
            if (detailIngredientViewModel.isIngredientChanged) {
                openDiscardChangesAlert()
                false
            } else {
                true
            }
        }
    }

    private fun setFormWatchers() {
        binding.apply {
            fieldUpdateMap.keys.forEach { editText ->
                editText.addTextChangedListener(universalTextWatcher)
            }
            fragmentDetailIngredientEditCarbs.addTextChangedListener(createDecimalLimiter(nutritionInfoDecimalLimit))
            fragmentDetailIngredientEditFat.addTextChangedListener(createDecimalLimiter(nutritionInfoDecimalLimit))
            fragmentDetailIngredientEditProtein.addTextChangedListener(createDecimalLimiter(nutritionInfoDecimalLimit))
            fragmentDetailIngredientEditPrice.addTextChangedListener(createDecimalLimiter(nutritionInfoPriceDecimalLimit))

            fragmentDetailIngredientEditMeasurementValue.addTextChangedListener(onMeasurementValueChangedWatcher)
            fragmentDetailIngredientEditSpinnerMeasurementType.onItemSelectedListener = onMeasurementTypeItemSelectedListener
        }
    }

    private fun setView(ingredient: Ingredient) {
        binding.apply {
            if (detailIngredientViewModel.isCreatingIngredient && detailIngredientViewModel.isCreatingFirstLoad) {
                detailIngredientViewModel.isCreatingFirstLoadDone()
                fragmentDetailIngredientEditIdentifier.setText("")
                fragmentDetailIngredientEditCalories.setText("")
                fragmentDetailIngredientEditCarbs.setText("")
                fragmentDetailIngredientEditFat.setText("")
                fragmentDetailIngredientEditProtein.setText("")
                fragmentDetailIngredientEditMeasurementValue.setText("")
                fragmentDetailIngredientEditPrice.setText("")
                fragmentDetailIngredientEditSpinnerMeasurementType.setSelection(0)
            } else {
                fragmentDetailIngredientEditIdentifier.setText(ingredient.identifier)
                fragmentDetailIngredientEditMeasurementValue.setText(ingredient.measurementValueEntered.toString())
                fragmentDetailIngredientEditSpinnerMeasurementType.setSelection(ingredient.measurementType.ordinal)

                val nutritionInfo = ingredient.embeddedNutritionInfo
                fragmentDetailIngredientEditCalories.setText(nutritionInfo.calories.toCalories())
                fragmentDetailIngredientEditCarbs.setText(nutritionInfo.carbohydratesInGrams.toOneDecimal())
                fragmentDetailIngredientEditFat.setText(nutritionInfo.fatInGrams.toOneDecimal())
                fragmentDetailIngredientEditProtein.setText(nutritionInfo.proteinInGrams.toOneDecimal())
                fragmentDetailIngredientEditPrice.setText(nutritionInfo.price.toPrice())
            }
        }
    }

    private fun openDiscardChangesAlert() {
        var isDismissedByButton = false

        val alertDialog = createDiscardAlertDialog(
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
        val actionId = if (detailIngredientViewModel.isCreatingIngredient) {
            R.id.searchFragment
        } else {
            R.id.detailIngredientFragment
        }
        findNavController().popBackStack(actionId, false)
    }

    private fun validateFields(): Boolean {
        var isValid = true

        binding.apply {
            val editTexts = mutableListOf<TextInputEditText>()
            val textInputLayouts = mutableListOf<TextInputLayout>()

            editTexts.add(fragmentDetailIngredientEditIdentifier)
            editTexts.add(fragmentDetailIngredientEditCalories)
            editTexts.add(fragmentDetailIngredientEditCarbs)
            editTexts.add(fragmentDetailIngredientEditFat)
            editTexts.add(fragmentDetailIngredientEditProtein)
            editTexts.add(fragmentDetailIngredientEditPrice)
            editTexts.add(fragmentDetailIngredientEditMeasurementValue)

            textInputLayouts.add(fragmentDetailIngredientEditIdentifier.parent?.parent as TextInputLayout)
            textInputLayouts.add(fragmentDetailIngredientEditCalories.parent?.parent as TextInputLayout)
            textInputLayouts.add(fragmentDetailIngredientEditCarbs.parent?.parent as TextInputLayout)
            textInputLayouts.add(fragmentDetailIngredientEditFat.parent?.parent as TextInputLayout)
            textInputLayouts.add(fragmentDetailIngredientEditProtein.parent?.parent as TextInputLayout)
            textInputLayouts.add(fragmentDetailIngredientEditPrice.parent?.parent as TextInputLayout)
            textInputLayouts.add(fragmentDetailIngredientEditMeasurementValue.parent?.parent as TextInputLayout)

            for ((index, editText) in editTexts.withIndex()) {
                val textLayout = textInputLayouts[index]
                if (editText.text.isNullOrBlank()) {
                    textLayout.error = getString(R.string.error_field_required)
                    isValid = false
                } else {
                    textLayout.error = null
                }
            }
        }

        return isValid
    }

    private fun resetMeasurementValueToDefault() {
        val measurementValueInput = binding.fragmentDetailIngredientEditMeasurementValue
        measurementValueInput.setText("1")
        measurementValueInput.setSelection(1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireMainActivity().hideActionSave()
    }
}