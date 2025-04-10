package com.cashruler

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.cashruler.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.text.NumberFormat
import java.util.*

@HiltAndroidTest
class EndToEndTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val testRule: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(composeTestRule)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun completeUserJourney() {
        // Ajouter une dépense
        composeTestRule.onNodeWithContentDescription("Ajouter une dépense").performClick()
        
        composeTestRule.onNodeWithTag("titre_depense")
            .performTextInput("Courses au supermarché")
        
        composeTestRule.onNodeWithTag("montant_depense")
            .performTextInput("75.50")
        
        composeTestRule.onNodeWithTag("categorie_depense")
            .performClick()
        composeTestRule.onNodeWithText("Alimentation").performClick()
        
        composeTestRule.onNodeWithText("Sauvegarder").performClick()

        // Vérifier que la dépense apparaît dans la liste
        composeTestRule.onNodeWithText("Courses au supermarché").assertIsDisplayed()
        composeTestRule.onNodeWithText(formatMontant(75.50)).assertIsDisplayed()

        // Aller à l'écran des statistiques
        composeTestRule.onNodeWithContentDescription("Statistiques").performClick()
        
        // Vérifier que les dépenses sont mises à jour
        composeTestRule.onNodeWithText("Dépenses totales")
            .onChildren()
            .filterToOne(hasText(formatMontant(75.50)))
            .assertExists()

        // Créer un objectif d'épargne
        composeTestRule.onNodeWithContentDescription("Épargne").performClick()
        composeTestRule.onNodeWithContentDescription("Nouveau projet").performClick()

        composeTestRule.onNodeWithTag("titre_projet")
            .performTextInput("Vacances d'été")
        
        composeTestRule.onNodeWithTag("montant_objectif")
            .performTextInput("1000")

        composeTestRule.onNodeWithText("Créer").performClick()

        // Vérifier que le projet apparaît
        composeTestRule.onNodeWithText("Vacances d'été").assertIsDisplayed()
        composeTestRule.onNodeWithText(formatMontant(1000.0)).assertIsDisplayed()

        // Ajouter une transaction d'épargne
        composeTestRule.onNodeWithText("Vacances d'été").performClick()
        composeTestRule.onNodeWithContentDescription("Ajouter un versement").performClick()

        composeTestRule.onNodeWithTag("montant_versement")
            .performTextInput("200")
        
        composeTestRule.onNodeWithText("Ajouter").performClick()

        // Vérifier la progression
        composeTestRule.onNodeWithTag("progression")
            .assertIsDisplayed()
            .onChildren()
            .filterToOne(hasContentDescription("20%"))
            .assertExists()

        // Retour au tableau de bord
        composeTestRule.onNodeWithContentDescription("Accueil").performClick()

        // Vérifier le résumé
        composeTestRule.onNodeWithText("Solde disponible")
            .onChildren()
            .filterToOne(hasText(formatMontant(-75.50)))
            .assertExists()
    }

    private fun formatMontant(montant: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.FRANCE)
        return format.format(montant)
    }
}
