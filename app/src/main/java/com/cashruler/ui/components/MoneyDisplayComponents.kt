package com.cashruler.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

/**
 * Affiche un montant avec animation et formatage
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMoneyDisplay(
    amount: Double,
    modifier: Modifier = Modifier,
    currencyCode: String = Currency.getInstance(Locale.getDefault()).currencyCode,
    isPositiveGood: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    animationDuration: Int = 500
) {
    val formattedAmount = remember(amount, currencyCode) {
        formatAmount(amount, currencyCode)
    }

    val color = when {
        amount > 0 -> if (isPositiveGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        amount < 0 -> if (isPositiveGood) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = formattedAmount,
        style = textStyle,
        color = color,
        modifier = modifier,
        textAlign = TextAlign.End
    )
}

/**
 * Affiche un montant avec une étiquette et des détails optionnels
 */
@Composable
fun MoneyDisplayCard(
    amount: Double,
    label: String,
    modifier: Modifier = Modifier,
    details: String? = null,
    currencyCode: String = Currency.getInstance(Locale.getDefault()).currencyCode,
    isPositiveGood: Boolean = true,
    showTrend: Boolean = false,
    previousAmount: Double? = null
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
            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Montant principal
            AnimatedMoneyDisplay(
                amount = amount,
                currencyCode = currencyCode,
                isPositiveGood = isPositiveGood,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            // Détails optionnels
            if (details != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tendance
            if (showTrend && previousAmount != null) {
                Spacer(modifier = Modifier.height(8.dp))
                MoneyTrendIndicator(
                    currentAmount = amount,
                    previousAmount = previousAmount,
                    currencyCode = currencyCode
                )
            }
        }
    }
}

/**
 * Affiche un indicateur de tendance entre deux montants
 */
@Composable
fun MoneyTrendIndicator(
    currentAmount: Double,
    previousAmount: Double,
    currencyCode: String = Currency.getInstance(Locale.getDefault()).currencyCode
) {
    val difference = currentAmount - previousAmount
    val percentChange = if (previousAmount != 0.0) {
        (difference / previousAmount) * 100
    } else 0.0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (difference >= 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = if (difference >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Text(
            text = "${formatAmount(difference, currencyCode)} (${String.format("%.1f", percentChange)}%)",
            style = MaterialTheme.typography.bodySmall,
            color = if (difference >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Formatte un montant avec la devise spécifiée
 */
private fun formatAmount(amount: Double, currencyCode: String): String {
    val format = NumberFormat.getCurrencyInstance()
    format.currency = Currency.getInstance(currencyCode)
    format.minimumFractionDigits = 2
    format.maximumFractionDigits = 2
    return format.format(amount)
}

@Preview(showBackground = true)
@Composable
fun MoneyDisplayPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MoneyDisplayCard(
                amount = 1234.56,
                label = "Revenu mensuel",
                details = "Pour janvier 2025",
                showTrend = true,
                previousAmount = 1000.0
            )

            MoneyDisplayCard(
                amount = -567.89,
                label = "Dépenses",
                isPositiveGood = false
            )

            MoneyDisplayCard(
                amount = 5000.0,
                label = "Épargne",
                details = "Objectif: 10 000 €"
            )
        }
    }
}
