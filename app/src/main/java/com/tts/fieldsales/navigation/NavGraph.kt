package com.tts.fieldsales.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.tts.fieldsales.data.prefs.AppPreferences
import com.tts.fieldsales.ui.screen.*

@Composable
fun AppNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var startDest by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDest = if (prefs.isLoggedIn()) Screen.Dashboard.route else Screen.Login.route
    }

    if (startDest == null) return

    NavHost(navController = navController, startDestination = startDest!!) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(onNavigate = { route -> navController.navigate(route) })
        }

        composable(Screen.Orders.route) {
            OrdersScreen(
                onNavigateBack = { navController.popBackStack() },
                onOrderDetail = { id -> navController.navigate(Screen.OrderDetail.createRoute(id)) },
                onNewOrder = { navController.navigate(Screen.NewOrder.route) }
            )
        }

        composable(
            Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStack ->
            val orderId = backStack.arguments?.getInt("orderId") ?: return@composable
            OrderDetailScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() },
                onPreview = { reportName, recordId, recordName ->
                    navController.navigate(Screen.PrintPreview.createRoute(reportName, recordId, recordName))
                }
            )
        }

        composable(Screen.NewOrder.route) {
            NewOrderScreen(onBack = { navController.popBackStack() }, onOrderCreated = { id ->
                navController.navigate(Screen.OrderDetail.createRoute(id)) { popUpTo(Screen.NewOrder.route) { inclusive = true } }
            })
        }

        composable(Screen.Invoices.route) {
            InvoicesScreen(
                onBack = { navController.popBackStack() },
                onInvoiceDetail = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) }
            )
        }

        composable(
            Screen.InvoiceDetail.route,
            arguments = listOf(navArgument("invoiceId") { type = NavType.IntType })
        ) { backStack ->
            val invoiceId = backStack.arguments?.getInt("invoiceId") ?: return@composable
            InvoiceDetailScreen(
                invoiceId = invoiceId,
                onBack = { navController.popBackStack() },
                onPreview = { reportName, recordId, recordName ->
                    navController.navigate(Screen.PrintPreview.createRoute(reportName, recordId, recordName))
                }
            )
        }

        composable(Screen.Payments.route) {
            PaymentsScreen(
                onBack = { navController.popBackStack() },
                onNewPayment = { navController.navigate(Screen.NewPayment.route) }
            )
        }

        composable(Screen.NewPayment.route) {
            NewPaymentScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Returns.route) {
            ReturnsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Customers.route) {
            CustomersScreen(
                onBack = { navController.popBackStack() },
                onCustomerDetail = { id -> navController.navigate(Screen.CustomerDetail.createRoute(id)) },
                onNewCustomer = { navController.navigate(Screen.NewCustomer.route) }
            )
        }

        composable(
            Screen.CustomerDetail.route,
            arguments = listOf(navArgument("customerId") { type = NavType.IntType })
        ) { backStack ->
            val customerId = backStack.arguments?.getInt("customerId") ?: return@composable
            CustomerDetailScreen(customerId = customerId, onBack = { navController.popBackStack() })
        }

        composable(Screen.NewCustomer.route) {
            NewCustomerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Routes.route) {
            RoutesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Expenses.route) {
            ExpensesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Attendance.route) {
            AttendanceScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Closing.route) {
            ClosingScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PrinterSetup.route) {
            PrinterSetupScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Screen.PrintPreview.route,
            arguments = listOf(
                navArgument("reportName") { type = NavType.StringType },
                navArgument("recordId")   { type = NavType.IntType },
                navArgument("recordName") { type = NavType.StringType }
            )
        ) { backStack ->
            val reportName  = java.net.URLDecoder.decode(backStack.arguments?.getString("reportName") ?: "", "UTF-8")
            val recordId    = backStack.arguments?.getInt("recordId") ?: 0
            val recordName  = java.net.URLDecoder.decode(backStack.arguments?.getString("recordName") ?: "Document", "UTF-8")
            PrintPreviewScreen(
                reportName  = reportName,
                recordId    = recordId,
                recordName  = recordName,
                onBack      = { navController.popBackStack() }
            )
        }
    }
}
