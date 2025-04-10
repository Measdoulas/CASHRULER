package com.cashruler.ui.screens.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cashruler.data.models.Expense
import com.cashruler.data.models.SpendingLimit
import com.cashruler.ui.BaseComposeTest
import com.cashruler.ui.viewmodels.DashboardViewModel
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import java.util.*

@HiltAndroidTest
class DashboardScreenTest : BaseComposeTest() {

    private lateinit var viewModel: DashboardViewModel

    override fun setup() {
        super.setup()
        viewModel = mockk(relaxed = true)

        // État initial simulé
        every { viewModel.uiState } returns MutableStateFlow(
            DashboardUiState(
                totalExpenses = 1500.0,
                totalIncome = 3000.0,
                recentExpenses = listOf(
                    createExpense(1, "Courses", 100.0),
                    createExpense(2, "Restaurant", 50.0)
                ),
                activeLimits = listOf(
                    createSpendingLimit(1, "Alimentation", 500.0, 300.0),
                    createSpendingLimit(2, "Transport", 200.0, 150.0)
                ),
                isLoading = false
            )
        )
    }

    @Test
    fun dashboardShowsCorrectInitialState() {
        composeTestRule.setContent {
            DashboardScreen(
                onNavigateToExpenses = {},
                onNavigateToIncome = {},
                onNavigateToSavings = {},
                onNavigateToSettings = {},
                viewModel = viewModel
            )
        }

        // Vérifier que le solde est affiché
        composeTestRule
            .onNodeWithText("Solde disponible")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("1 500,00 €")
            .assertIsDisplayed()

        // Vérifier que les dépenses récentes sont affichées
        composeTestRule
            .onNodeWithText("Courses")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Restaurant")
            .assertIsDisplayed()

        // Vérifier que les limites sont affichées
        composeTestRule
            .onNodeWithText("Alimentation")
            .assertIsDisplayed()
            .onParent()
            .onChildren()
            .assertAny(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.6f, 0f..1f)))
    }

    @Test
    fun clickingOnExpenseNavigatesToExpenseScreen() {
        var navigationCalled = false
        
        composeTestRule.setContent {
            DashboardScreen(
                onNavigateToExpenses = { navigationCalled = true },
                onNavigateToIncome = {},
                onNavigateToSavings = {},
                onNavigateToSettings = {},
                viewModel = viewModel
            )
        }

        composeTestRule
            .onNodeWithText("Courses")
            .performClick()

        assert(navigationCalled)
    }

    @Test
    fun showsLoadingIndicatorWhenLoading() {
        every { viewModel.uiState } returns MutableStateFlow(
            DashboardUiState(isLoading = true)
        )

        composeTestRule.setContent {
            DashboardScreen(
                onNavigateToExpenses = {},
                onNavigateToIncome = {},
                onNavigateToSavings = {},
                onNavigateToSettings = {},
                viewModel = viewModel
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Chargement")
            .assertIsDisplayed()
    }

    private fun createExpense(
        id: Long,
        title: String,
        amount: Double,
        date: Date = Date()
    ) = Expense(
        id = id,
        title = title,
        amount = amount,
        category = "Test",
        date = date
    )

    private fun createSpendingLimit(
        id: Long,
        category: String,
        amount: Double,
        currentAmount: Double
    ) = SpendingLimit(
        id = id,
        category = category,
        amount = amount,
        currentAmount = currentAmount,
        startDate = Date(),
        frequency = com.cashruler.data.models.SpendingLimitFrequency.MONTHLY
    )
}
