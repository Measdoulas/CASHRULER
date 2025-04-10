package com.cashruler.ui.screens.statistics

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
 * Carte de synthèse de période
 */
@Composable
fun PeriodSummaryCard(
    income: Double,
    expenses: Double,
    savings: Double,
    periodLabel: String,
    modifier: Modifier = Modifier,
    previousPeriodIncome: Double? = null,
    previousPeriodExpenses: Double? = null,
    previousPeriodSavings: Double? = null
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
                text = periodLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Revenus
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Revenus",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AnimatedMoneyDisplay(
                        amount = income,
                        textStyle = MaterialTheme.typography.titleMedium
                    )
                    if (previousPeriodIncome != null) {
                        MoneyTrendIndicator(
                            currentAmount = income,
                            previousAmount = previousPeriodIncome
                        )
                    }
                }

                // Dépenses
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dépenses",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AnimatedMoneyDisplay(
                        amount = -expenses,
                        textStyle = MaterialTheme.typography.titleMedium,
                        isPositiveGood = false
                    )
                    if (previousPeriodExpenses != null) {
                        MoneyTrendIndicator(
                            currentAmount = -expenses,
                            previousAmount = -previousPeriodExpenses,
                            isPositiveGood = false
                        )
                    }
                }

                // Épargne
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Épargne",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AnimatedMoneyDisplay(
                        amount = savings,
                        textStyle = MaterialTheme.typography.titleMedium
                    )
                    if (previousPeriodSavings != null) {
                        MoneyTrendIndicator(
                            currentAmount = savings,
                            previousAmount = previousPeriodSavings
                        )
                    }
                }
            }
        }
    }
}

/**
 * Graphique de répartition des dépenses
 */
@Composable
fun ExpenseDistributionChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    title: String = "Répartition des dépenses"
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            DonutChart(
                data = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
    }
}

/**
 * Graphique d'évolution des dépenses
 */
@Composable
fun ExpenseTrendChart(
    monthlyExpenses: List<Pair<Date, Double>>,
    monthlyBudgets: List<Pair<Date, Double>>? = null,
    modifier: Modifier = Modifier,
    title: String = "Évolution des dépenses"
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // TODO: Implémenter un graphique en ligne pour l'évolution
            // Placeholder pour le moment
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Graphique d'évolution à venir")
            }
        }
    }
}

/**
 * Indicateur de réalisation d'objectifs
 */
@Composable
fun GoalProgressIndicator(
    currentValue: Double,
    targetValue: Double,
    title: String,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true
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
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showPercentage) {
                    CircularProgressChart(
                        progress = (currentValue / targetValue).toFloat(),
                        modifier = Modifier.size(80.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatAmount(currentValue),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "sur ${formatAmount(targetValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
fun StatisticsComponentsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodSummaryCard(
                income = 3000.0,
                expenses = 2000.0,
                savings = 1000.0,
                periodLabel = "Mars 2025",
                previousPeriodIncome = 2800.0,
                previousPeriodExpenses = 1800.0,
                previousPeriodSavings = 1000.0
            )

            ExpenseDistributionChart(
                data = mapOf(
                    "Alimentation" to 500.0,
                    "Transport" to 300.0,
                    "Loisirs" to 200.0,
                    "Logement" to 800.0
                )
            )

            GoalProgressIndicator(
                currentValue = 1500.0,
                targetValue = 2000.0,
                title = "Objectif d'épargne"
            )
        }
    }
}
