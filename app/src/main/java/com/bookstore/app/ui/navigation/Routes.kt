package com.bookstore.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val CATALOGUE = "catalogue"
    const val BASKET = "basket"
    const val ORDERS = "orders"

    const val CATEGORY = "category/{categoryId}"
    fun category(id: Long) = "category/$id"

    const val PRODUCT = "product/{bookId}"
    fun product(id: Long) = "product/$id"

    const val ADDRESS_SELECT = "checkout/address"
    const val ADDRESS_EDIT = "checkout/address/edit/{addressId}"
    fun addressEdit(id: Long) = "checkout/address/edit/$id"

    const val PAYMENT = "checkout/payment/{addressId}"
    fun payment(addressId: Long) = "checkout/payment/$addressId"

    const val CONFIRMATION = "confirmation/{orderId}"
    fun confirmation(orderId: String) = "confirmation/$orderId"

    const val ORDER = "order/{orderId}"
    fun order(orderId: String) = "order/$orderId"

    val tabs = setOf(HOME, CATALOGUE, BASKET, ORDERS)
}
