package com.bookstore.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookstore.app.ui.auth.LoginScreen
import com.bookstore.app.ui.auth.RegisterScreen
import com.bookstore.app.ui.basket.BasketScreen
import com.bookstore.app.ui.catalogue.CatalogueScreen
import com.bookstore.app.ui.catalogue.CategoriesScreen
import com.bookstore.app.ui.catalogue.ProductScreen
import com.bookstore.app.ui.checkout.AddressEditScreen
import com.bookstore.app.ui.checkout.AddressSelectScreen
import com.bookstore.app.ui.checkout.PaymentScreen
import com.bookstore.app.ui.common.AppUi
import com.bookstore.app.ui.common.LocalAppUi
import com.bookstore.app.ui.common.appViewModel
import com.bookstore.app.ui.home.HomeScreen
import com.bookstore.app.ui.orders.ConfirmationScreen
import com.bookstore.app.ui.orders.OrderDetailScreen
import com.bookstore.app.ui.orders.OrdersScreen

/** Signed-out users only ever see the auth graph; signing in or out swaps the whole graph. */
@Composable
fun AppRoot(isSignedIn: Boolean) {
    if (isSignedIn) MainGraph() else AuthGraph()
}

@Composable
private fun AuthGraph() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { LoginScreen(onRegister = { nav.navigate(Routes.REGISTER) }) }
        composable(Routes.REGISTER) {
            RegisterScreen(onLogin = { nav.popBackStack() })
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabItems = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.CATALOGUE, "Catalogue", Icons.AutoMirrored.Filled.LibraryBooks),
    Tab(Routes.BASKET, "Basket", Icons.Filled.ShoppingBasket),
    Tab(Routes.ORDERS, "Orders", Icons.AutoMirrored.Filled.ReceiptLong)
)

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainGraph() {
    val nav = rememberNavController()
    val vm = appViewModel { MainViewModel(it.cartRepository, it.authRepository) }
    val cartCount by vm.cartCount.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val appUi = remember(nav) {
        AppUi(snackbar, openBasket = { nav.openTab(Routes.BASKET) }, logout = vm::logout)
    }
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route

    CompositionLocalProvider(LocalAppUi provides appUi) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentRoute in Routes.tabs) {
                    NavigationBar {
                        tabItems.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { nav.openTab(tab.route) },
                                icon = {
                                    if (tab.route == Routes.BASKET && cartCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$cartCount") } }) { Icon(tab.icon, null) }
                                    } else Icon(tab.icon, null)
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { padding: PaddingValues ->
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenCategory = { nav.navigate(Routes.category(it)) },
                        onOpenProduct = { nav.navigate(Routes.product(it)) },
                        onOpenOrders = { nav.openTab(Routes.ORDERS) },
                        onOpenOrder = { nav.navigate(Routes.order(it)) }
                    )
                }
                composable(Routes.CATALOGUE) {
                    CategoriesScreen(onOpenCategory = { nav.navigate(Routes.category(it)) })
                }
                composable(
                    Routes.CATEGORY,
                    arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
                ) { entry ->
                    CatalogueScreen(
                        initialCategoryId = entry.arguments!!.getLong("categoryId"),
                        onBack = { nav.popBackStack() },
                        onOpenProduct = { nav.navigate(Routes.product(it)) }
                    )
                }
                composable(
                    Routes.PRODUCT,
                    arguments = listOf(navArgument("bookId") { type = NavType.LongType })
                ) { entry ->
                    ProductScreen(
                        bookId = entry.arguments!!.getLong("bookId"),
                        onBack = { nav.popBackStack() },
                        onOpenProduct = { nav.navigate(Routes.product(it)) }
                    )
                }
                composable(Routes.BASKET) {
                    BasketScreen(
                        onCheckout = { nav.navigate(Routes.ADDRESS_SELECT) },
                        onBrowse = { nav.openTab(Routes.CATALOGUE) },
                        onOpenProduct = { nav.navigate(Routes.product(it)) }
                    )
                }
                composable(Routes.ADDRESS_SELECT) {
                    AddressSelectScreen(
                        onBack = { nav.popBackStack() },
                        onAddOrEdit = { nav.navigate(Routes.addressEdit(it)) },
                        onContinue = { nav.navigate(Routes.payment(it)) }
                    )
                }
                composable(
                    Routes.ADDRESS_EDIT,
                    arguments = listOf(navArgument("addressId") { type = NavType.LongType })
                ) { entry ->
                    AddressEditScreen(
                        addressId = entry.arguments!!.getLong("addressId"),
                        onDone = { nav.popBackStack() }
                    )
                }
                composable(
                    Routes.PAYMENT,
                    arguments = listOf(navArgument("addressId") { type = NavType.LongType })
                ) { entry ->
                    PaymentScreen(
                        addressId = entry.arguments!!.getLong("addressId"),
                        onBack = { nav.popBackStack() },
                        onChangeAddress = { nav.popBackStack() },
                        onPaid = { orderId ->
                            nav.navigate(Routes.confirmation(orderId)) {
                                popUpTo(Routes.BASKET) { inclusive = false }
                            }
                        }
                    )
                }
                composable(
                    Routes.CONFIRMATION,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                ) { entry ->
                    ConfirmationScreen(
                        orderId = entry.arguments!!.getString("orderId")!!,
                        onViewOrder = { nav.navigate(Routes.order(it)) },
                        onContinueShopping = { nav.openTab(Routes.HOME) }
                    )
                }
                composable(Routes.ORDERS) {
                    OrdersScreen(
                        onOpenOrder = { nav.navigate(Routes.order(it)) },
                        onBrowse = { nav.openTab(Routes.CATALOGUE) }
                    )
                }
                composable(
                    Routes.ORDER,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                ) { entry ->
                    OrderDetailScreen(
                        orderId = entry.arguments!!.getString("orderId")!!,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
