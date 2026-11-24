package com.bookstore.app

import android.content.Context
import androidx.room.Room
import com.bookstore.app.data.auth.AuthRepository
import com.bookstore.app.data.auth.SessionManager
import com.bookstore.app.data.local.BookstoreDatabase
import com.bookstore.app.data.payment.PaymentGateway
import com.bookstore.app.data.payment.SimulatedPaymentGateway
import com.bookstore.app.data.repo.AddressRepository
import com.bookstore.app.data.repo.CartRepository
import com.bookstore.app.data.repo.CatalogRepository
import com.bookstore.app.data.repo.OrderRepository
import com.bookstore.app.data.repo.RecommendationRepository

/** Manual dependency container: keeps each module (auth, catalogue, basket, orders, payment) separate. */
class AppContainer(context: Context) {
    val database: BookstoreDatabase =
        Room.databaseBuilder(context, BookstoreDatabase::class.java, "bookstore.db").build()

    val session = SessionManager(context)
    val paymentGateway: PaymentGateway = SimulatedPaymentGateway()

    val authRepository = AuthRepository(database.userDao(), session)
    val catalogRepository = CatalogRepository(database.catalogDao(), database.browseDao(), session)
    val cartRepository = CartRepository(database.cartDao(), database.catalogDao(), session)
    val addressRepository = AddressRepository(database.addressDao(), session)
    val orderRepository = OrderRepository(database, session, paymentGateway)
    val recommendationRepository = RecommendationRepository(catalogRepository, orderRepository, cartRepository)
}
