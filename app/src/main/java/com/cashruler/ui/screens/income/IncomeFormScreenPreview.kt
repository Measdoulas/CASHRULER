package com.cashruler.ui.screens.income

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.cashruler.data.models.Income
import com.cashruler.ui.theme.CashRulerTheme
import java.util.Date

@Preview(
    name = "Nouveau revenu",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun IncomeFormScreenPreviewNew() {
    CashRulerTheme {
        IncomeFormScreen(
            incomeId = null,
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Modification revenu",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun IncomeFormScreenPreviewEdit(
    @PreviewParameter(SampleIncomeProvider::class) income: Income
) {
    CashRulerTheme {
        IncomeFormScreen(
            incomeId = income.id,
            onNavigateBack = {}
        )
    }
}

class SampleIncomeProvider : PreviewParameterProvider<Income> {
    override val values = sequenceOf(
        Income(
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
        Income(
            id = 2L,
            amount = 75000.0,
            description = "Prestation freelance",
            type = "Freelance",
            date = Date(),
            isRecurring = false,
            isTaxable = true,
            taxRate = 20.0
        ),
        Income(
            id = 3L,
            amount = 100000.0,
            description = "Loyer appartement",
            type = "Location",
            date = Date(),
            isRecurring = true,
            recurringFrequency = 30,
            isTaxable = true,
            taxRate = 25.0,
            notes = "Charges comprises"
        )
    )
}

@Preview(
    name = "État de chargement",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun IncomeFormScreenPreviewLoading() {
    CashRulerTheme {
        LoadingState(message = "Chargement du revenu...")
    }
}

@Preview(
    name = "Message de succès",
    showBackground = true
)
@Composable
fun IncomeFormScreenPreviewSuccess() {
    CashRulerTheme {
        MessageSnackbar(
            message = "Revenu ajouté avec succès",
            type = MessageType.SUCCESS,
            onDismiss = {}
        )
    }
}

@Preview(
    name = "Message d'erreur",
    showBackground = true
)
@Composable
fun IncomeFormScreenPreviewError() {
    CashRulerTheme {
        MessageSnackbar(
            message = "Une erreur est survenue lors de l'enregistrement",
            type = MessageType.ERROR,
            onDismiss = {}
        )
    }
}
