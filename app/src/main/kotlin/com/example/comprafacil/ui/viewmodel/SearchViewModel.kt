package com.example.comprafacil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comprafacil.core.data.Product
import com.example.comprafacil.core.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: ProductRepository = ProductRepository()) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.length >= 2) {
            searchProducts(newQuery)
        } else {
            _searchResults.value = emptyList()
        }
    }

    private fun searchProducts(query: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // For now, we fetch all and filter client-side as the repository doesn't have a specific search method yet,
                // but we can improve this later.
                val allProducts = repository.getProducts()
                _searchResults.value = allProducts.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.description?.contains(query, ignoreCase = true) == true
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
