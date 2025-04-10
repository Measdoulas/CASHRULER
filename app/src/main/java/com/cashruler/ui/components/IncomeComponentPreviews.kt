package com.cashruler.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cashruler.data.models.Income
import com.cashruler.ui.theme.CashRulerTheme
import java.util.*

@Preview(name = "Revenu simple", showBackground = true)
@Composable
fun IncomeListItemPreviewSimple() {
    CashRulerTheme {
        IncomeListItem(
            income = Income(
                id = 1L,
                amount = 450000.0,
                description = "Salaire Février",
                type = "Salaire",
                date = Date(),
                notes = "Prime de performance incluse"
            ),
            onClick = {},
            onDelete = {}
        )
    }
}

@Preview(name = "Revenu récurrent", showBackground = true)
@Composable
fun IncomeListItemPreviewRecurring() {
    CashRulerTheme {
        IncomeListItem(
            income = Income(
                id = 2L,
                amount = 100000.0,
                description = "Loyer appartement",
                type = "Location",
                date = Date(),
                isRecurring = true,
                recurringFrequency = 30,
                isTaxable = true,
                taxRate = 25.0,
                notes = "Charges comprises"
            ),
            onClick = {},
            onDelete = {}
        )
    }
}

@Preview(name = "Revenu imposable", showBackground = true)
@Composable
fun IncomeListItemPreviewTaxable() {
    CashRulerTheme {
        IncomeListItem(
            income = Income(
                id = 3L,
                amount = 75000.0,
                description = "Prestation freelance",
                type = "Freelance",
                date = Date(),
                isTaxable = true,
                taxRate = 20.0
            ),
            onClick = {},
            onDelete = {}
        )
    }
}

@Preview(name = "Résumé des revenus", showBackground = true)
@Composable
fun IncomeSummaryCardPreview() {
    CashRulerTheme {
        IncomeSummaryCard(
            totalIncome = 625000.0,
            totalNetIncome = 500000.0,
            totalTaxes = 125000.0,
            recurring = 2
        )
    }
}

@Preview(name = "Liste de revenus", showBackground = true)
@Composable
fun IncomeListPreview() {
    CashRulerTheme {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IncomeSummaryCard(
                totalIncome = 625000.0,
                totalNetIncome = 500000.0,
                totalTaxes = 125000.0,
                recurring = 2
            )

            ListSection(
                title = "Revenus récurrents",
                subtitle = "Mensuels"
            )

            IncomeListItem(
                income = Income(
                    id = 1L,
                    amount = 450000.0,
                    description = "Salaire Février",
                    type = "Salaire",
                    date = Date(),
                    isRecurring = true,
                    recurringFrequency = 30,
                    isTaxable = true,
                    taxRate = 15.0,
                    notes = "Prime incluse"
                ),
                onClick = {},
                onDelete = {}
            )

            IncomeListItem(
                income = Income(
                    id = 2L,
                    amount = 100000.0,
                    description = "Loyer appartement",
                    type = "Location",
                    date = Date(),
                    isRecurring = true,
                    recurringFrequency = 30,
                    isTaxable = true,
                    taxRate = 25.0,
                    notes = "Charges comprises"
                ),
                onClick = {},
                onDelete = {}
            )

            ListSection(
                title = "Autres revenus"
            )

            IncomeListItem(
                income = Income(
                    id = 3L,
                    amount = 75000.0,
                    description = "Prestation freelance",
                    type = "Freelance",
                    date = Date(),
                    isTaxable = true,
                    taxRate = 20.0
                ),
                onClick = {},
                onDelete = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true)
@Composable
fun IncomeComponentsDarkPreview() {
    CashRulerTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IncomeSummaryCard(
                totalIncome = 625000.0,
                totalNetIncome = 500000.0,
                totalTaxes = 125000.0,
                recurring = 2
            )

            IncomeListItem(
                income = Income(
                    id = 1L,
                    amount = 450000.0,
                    description = "Salaire Février",
                    type = "Salaire",
                    date = Date(),
                    isRecurring = true,
                    recurringFrequency = 30,
                    isTaxable = true,
                    taxRate = 15.0,
                    notes = "Prime incluse"
                ),
                onClick = {},
                onDelete = {}
            )
        }
    }
}
