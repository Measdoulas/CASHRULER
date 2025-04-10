package com.cashruler.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashruler.ui.components.*
import java.util.*

/**
 * Carte de résumé mensuel
 */
@Composable
fun MonthlySummaryCard(
    income: Double,
    expenses: Double,
    balance: Double,
    modifier: Modifier = Modifier,
    previousMonthIncome: Double? = null,
    previousMonthExpenses: Double? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Résumé du mois",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Revenus",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AnimatedMoneyDisplay(
                        amount = income,
                        textStyle = MaterialTheme.typography.titleLarge
                    )
                    if (previousMonthIncome != null) {
                        MoneyTrendIndicator(
                            currentAmount = income,
                            previousAmount = previousMonthIncome
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dépenses",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AnimatedMoneyDisplay(
                        amount = -expenses,
                        textStyle = MaterialTheme.typography.titleLarge,
                        isPositiveGood = false
                    )
                    if (previousMonthExpenses != null) {
                        MoneyTrendIndicator(
                            currentAmount = -expenses,
                            previousAmount = -previousMonthExpenses,
                            isPositiveGood = false
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Solde",
                    style = MaterialTheme.typography.titleMedium
                )
                AnimatedMoneyDisplay(
                    amount = balance,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * Carte de limite de dépenses
 */
@Composable
fun SpendingLimitCard(
    category: String,
    currentAmount: Double,
    limitAmount: Double,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onCardClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium
                )
                AnimatedMoneyDisplay(
                    amount = currentAmount,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val progress = (currentAmount / limitAmount).toFloat().coerceIn(0f, 1f)
            LabeledProgressBar(
                progress = progress,
                label = "Budget: ${formatAmount(limitAmount)}",
                color = when {
                    progress >= 1f -> MaterialTheme.colorScheme.error
                    progress >= 0.8f -> MaterialTheme.colorScheme.warning
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

/**
 * Carte de projet d'épargne
 */
@Composable
fun SavingsProjectCard(
    title: String,
    currentAmount: Double,
    targetAmount: Double,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onCardClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            CircularProgressChart(
                progress = (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Objectif: ${formatAmount(targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedMoneyDisplay(
                    amount = currentAmount,
                    textStyle = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * Formatte un montant (à remplacer par l'utilitaire approprié)
 */
private fun formatAmount(amount: Double): String {
    return "%.2f €".format(amount)
}

@Preview(showBackground = true)
@Composable
fun DashboardComponentsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MonthlySummaryCard(
                income = 3000.0,
                expenses = 2000.0,
                balance = 1000.0,
                previousMonthIncome = 2800.0,
                previousMonthExpenses = 1800.0
            )

            SpendingLimitCard(
                category = "Restaurants",
                currentAmount = 180.0,
                limitAmount = 200.0
            )

            SavingsProjectCard(
                title = "Vacances",
                currentAmount = 1500.0,
                targetAmount = 2000.0
            )
        }
    }
}
