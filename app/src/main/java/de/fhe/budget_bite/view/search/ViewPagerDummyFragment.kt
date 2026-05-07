package de.fhe.budget_bite.view.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.fhe.budget_bite.R
import de.fhe.budget_bite.databinding.FragmentSearchPageDummyBinding

class ViewPagerDummyFragment : Fragment(R.layout.fragment_search_page_dummy) {
    private var _binding: FragmentSearchPageDummyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchPageDummyBinding.inflate(inflater)

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}