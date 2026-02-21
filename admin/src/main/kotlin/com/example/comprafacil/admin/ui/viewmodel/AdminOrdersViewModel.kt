package com.example.comprafacil.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comprafacil.core.data.Order
import com.example.comprafacil.core.data.OrderItem
import com.example.comprafacil.core.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminOrdersViewModel(private val repository: OrderRepository = OrderRepository()) : ViewModel() {
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        fetchOrders()
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _orders.value = repository.getOrders()
            } catch (e: Exception) {
                // handle error
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun updateStatus(orderId: String, status: String) {
        repository.updateOrderStatus(orderId, status, "Atualizado pelo administrador")
        fetchOrders()
    }

    suspend fun deleteOrder(orderId: String) {
        repository.deleteOrder(orderId)
        fetchOrders()
    }

    suspend fun getItems(orderId: String): List<OrderItem> {
        return repository.getOrderItems(orderId)
    }
}
