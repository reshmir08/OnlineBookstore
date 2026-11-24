package com.bookstore.app.ui.home

import androidx.lifecycle.viewModelScope
import com.bookstore.app.core.util.AppResult
import com.bookstore.app.data.auth.AuthRepository
import com.bookstore.app.data.local.CategoryEntity
import com.bookstore.app.data.local.OrderItemEntity
import com.bookstore.app.data.local.OrderStatus
import com.bookstore.app.data.local.OrderWithItems
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.CatalogRepository
import com.bookstore.app.data.repo.OrderRepository
import com.bookstore.app.data.repo.RecommendationRepository
import com.bookstore.app.domain.Recommendation
import com.bookstore.app.ui.common.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val userName: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val recentOrders: List<OrderWithItems> = emptyList(),
    val buyAgain: List<OrderItemEntity> = emptyList(),
    val recommendations: List<Recommendation> = emptyList()
)

class HomeViewModel(
    private val auth: AuthRepository,
    catalog: CatalogRepository,
    orders: OrderRepository,
    recommendations: RecommendationRepository,
    private val cart: CartRepository
) : BaseViewModel() {

    private val userName = flow { emit(auth.currentUser()?.name?.substringBefore(' ').orEmpty()) }

    val state: StateFlow<HomeState> = combine(
        catalog.categories(), orders.orders(), recommendations.recommendations(), catalog.allBooks(), userName
    ) { categories, orderList, recs, books, name ->
        val stockById = books.associate { it.id to it.stock }
        val active = orderList.filter { it.order.status != OrderStatus.CANCELLED }
        // Most recently bought distinct titles that can still be bought.
        val buyAgain = active.flatMap { it.items }
            .distinctBy { it.bookId }
            .filter { (stockById[it.bookId] ?: 0) > 0 }
            .take(10)
        HomeState(
            loading = false,
            userName = name,
            categories = categories,
            recentOrders = orderList.take(2),
            buyAgain = buyAgain,
            recommendations = recs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun addToBasket(bookId: Long) = addToBasket(cart, bookId)

    fun buyAgain(item: OrderItemEntity) {
        viewModelScope.launch {
            when (val result = cart.add(item.bookId, item.quantity)) {
                is AppResult.Success -> say("\"${item.title}\" added to basket", viewBasket = true)
                is AppResult.Failure -> say(result.message)
            }
        }
    }
}
