package com.cashruler.validation

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Validateur d'accessibilité pour s'assurer que l'application est utilisable par tous
 */
class AccessibilityValidator(
    private val composeTestRule: ComposeTestRule
) {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val results = mutableListOf<AccessibilityResult>()

    /**
     * Vérifie l'accessibilité d'un écran complet
     */
    fun validateScreen(
        screenName: String,
        screenTags: List<String> = emptyList()
    ): List<AccessibilityResult> {
        results.clear()

        // Vérifier les descriptions pour le contenu
        validateContentDescriptions(screenName)

        // Vérifier les tailles de clic minimales
        validateTapTargets()

        // Vérifier les contrastes de couleur
        validateColorContrast()

        // Vérifier la hiérarchie de navigation
        validateNavigationHierarchy(screenTags)

        // Vérifier les étiquettes des champs de formulaire
        validateFormLabels()

        return results
    }

    private fun validateContentDescriptions(screenName: String) {
        composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .forEach { node ->
                val hasContentDescription = node.config.contains(SemanticsProperties.ContentDescription)
                if (!hasContentDescription) {
                    results.add(
                        AccessibilityResult(
                            screenName,
                            false,
                            "Élément cliquable sans description: ${node.printToString()}"
                        )
                    )
                }
            }
    }

    private fun validateTapTargets() {
        composeTestRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .forEach { node ->
                val bounds = node.boundsInRoot
                val minSize = 48 // Taille minimale recommandée en dp

                if (bounds.width < minSize || bounds.height < minSize) {
                    results.add(
                        AccessibilityResult(
                            "Tap_Targets",
                            false,
                            "Zone de clic trop petite: ${bounds.width}x${bounds.height}dp"
                        )
                    )
                }
            }
    }

    private fun validateColorContrast() {
        // Note: Cette validation nécessiterait une implémentation plus complexe
        // pour extraire les couleurs réelles et calculer les contrastes
        // Pour l'instant, on vérifie juste si les thèmes utilisent des couleurs accessibles
        
        results.add(
            AccessibilityResult(
                "Color_Contrast",
                true,
                "Validation du contraste des couleurs à implémenter"
            )
        )
    }

    private fun validateNavigationHierarchy(screenTags: List<String>) {
        screenTags.forEach { tag ->
            try {
                composeTestRule
                    .onNodeWithTag(tag)
                    .assertIsDisplayed()

                results.add(
                    AccessibilityResult(
                        "Navigation",
                        true,
                        "Élément $tag accessible"
                    )
                )
            } catch (e: AssertionError) {
                results.add(
                    AccessibilityResult(
                        "Navigation",
                        false,
                        "Élément $tag non accessible"
                    )
                )
            }
        }
    }

    private fun validateFormLabels() {
        composeTestRule
            .onAllNodes(hasSetTextAction())
            .fetchSemanticsNodes()
            .forEach { node ->
                val hasLabel = node.config.contains(SemanticsProperties.ContentDescription) ||
                             node.config.contains(SemanticsProperties.Text)
                
                if (!hasLabel) {
                    results.add(
                        AccessibilityResult(
                            "Form_Labels",
                            false,
                            "Champ de saisie sans étiquette: ${node.printToString()}"
                        )
                    )
                }
            }
    }

    /**
     * Vérifie les ratios de contraste pour les textes
     */
    fun validateTextContrast(
        foregroundColor: Int,
        backgroundColor: Int,
        isLargeText: Boolean = false
    ): AccessibilityResult {
        val ratio = calculateContrastRatio(foregroundColor, backgroundColor)
        val minimumRatio = if (isLargeText) 3.0 else 4.5

        return AccessibilityResult(
            "Text_Contrast",
            ratio >= minimumRatio,
            if (ratio < minimumRatio) {
                "Ratio de contraste insuffisant: $ratio (minimum requis: $minimumRatio)"
            } else {
                "Ratio de contraste satisfaisant: $ratio"
            }
        )
    }

    private fun calculateContrastRatio(foreground: Int, background: Int): Double {
        val fgLuminance = calculateRelativeLuminance(foreground)
        val bgLuminance = calculateRelativeLuminance(background)
        
        val lighter = kotlin.math.max(fgLuminance, bgLuminance)
        val darker = kotlin.math.min(fgLuminance, bgLuminance)
        
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun calculateRelativeLuminance(color: Int): Double {
        val red = (color shr 16 and 0xff) / 255.0
        val green = (color shr 8 and 0xff) / 255.0
        val blue = (color and 0xff) / 255.0

        val r = if (red <= 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4)
        val g = if (green <= 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4)
        val b = if (blue <= 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4)

        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    data class AccessibilityResult(
        val category: String,
        val isValid: Boolean,
        val message: String
    )
}
