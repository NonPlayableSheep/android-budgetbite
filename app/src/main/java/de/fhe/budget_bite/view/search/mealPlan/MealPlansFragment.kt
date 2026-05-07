package de.fhe.budget_bite.view.search.mealPlan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.FragmentMealPlansBinding
import de.fhe.budget_bite.view.detail.meal_plan.DetailMealPlanViewModel

@AndroidEntryPoint
class MealPlansFragment : Fragment(R.layout.fragment_meal_plans) {
    private var _binding: FragmentMealPlansBinding? = null
    private val binding get() = _binding!!
    private var _adapter: MealPlanAdapter? = null
    private val adapter get() = _adapter!!
    private val mealPlansViewModel: MealPlansViewModel by activityViewModels()
    private val detailMealPlanViewModel: DetailMealPlanViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealPlansBinding.inflate(inflater)

        _adapter = MealPlanAdapter(
            ::onItemClick
        )
        binding.fragmentMealPlansRv.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mealPlansViewModel.mealPlanLiveData.observe(viewLifecycleOwner) {
            adapter.updateDataset(it)
        }

        binding.apply {
            fragmentMealPlansCreateBtn.setOnClickListener {
                detailMealPlanViewModel.createMealPlan()
                findNavController().navigate(R.id.action_searchFragment_to_detailMealPlanEditFragment)
            }
        }
    }

    private fun onItemClick(position: Int) {
        val clickedMealPlan = mealPlansViewModel.mealPlanLiveData.value!![position]
        detailMealPlanViewModel.loadMealPlanById(clickedMealPlan.id)
        findNavController().navigate(R.id.action_searchFragment_to_detailMealPlanFragment)
    }

    override fun onDestroyView() {
        _binding = null
        _adapter = null
        super.onDestroyView()
    }
}