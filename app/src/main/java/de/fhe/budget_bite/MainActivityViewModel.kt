package de.fhe.budget_bite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {
    private var saveActionCallback: (() -> Unit)? = null
    private var _isShowingSaveAction = MutableLiveData(false)
    val isShowingSaveAction: LiveData<Boolean> get() = _isShowingSaveAction
    private var navBackFragmentCallback: (() -> Boolean)? = null
    private val _topLevelDestinations = MutableLiveData(setOf(
        R.id.plannerFragment,
        R.id.searchFragment,
        R.id.moreFragment
    ))
    val topLevelDestinations: LiveData<Set<Int>> get() = _topLevelDestinations

    fun setSearchFragmentSecondLevelDestination() {
        val filteredDestinations = _topLevelDestinations.value!!.filter { it != R.id.searchFragment }.toSet()
        _topLevelDestinations.value = filteredDestinations
    }

    fun resetSearchFragmentTopLevelDestination() {
        _topLevelDestinations.value = setOf(
            R.id.plannerFragment,
            R.id.searchFragment,
            R.id.moreFragment
        )
    }

    fun showActionSaveWithCallback(saveAction: () -> Unit) {
        saveActionCallback = saveAction
        _isShowingSaveAction.value = true
    }

    fun hideActionSave() {
        _isShowingSaveAction.value = false
    }

    fun invokeSaveActionCallback() {
        saveActionCallback?.invoke()
    }

    fun setFragmentBackPressedCallback(callback: () -> Boolean) {
        navBackFragmentCallback = {
            val shouldNavigate = callback.invoke()
            clearFragmentBackPressedCallback()
            shouldNavigate
        }
    }

    fun clearFragmentBackPressedCallback() {
        navBackFragmentCallback = null
    }

    fun invokeNavBackFragmentCallback(): Boolean {
        return navBackFragmentCallback?.invoke() ?: true
    }
}