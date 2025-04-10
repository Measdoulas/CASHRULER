package com.cashruler.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilitaires de formatage pour l'application
 */
object Formatters {
    /**
     * Formateur de monnaie par défaut
     */
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(Locale.getDefault())
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /**
     * Formateur de date courte (JJ/MM/AAAA)
     */
    private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    /**
     * Formateur de date longue avec heure
     */
    private val longDateFormat = SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.getDefault())

    /**
     * Formateur de mois et année
     */
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    /**
     * Formatte un montant en devise locale
     */
    fun formatAmount(amount: Double, showPositive: Boolean = false): String {
        val formattedAmount = currencyFormatter.format(amount)
        return if (showPositive && amount > 0) "+$formattedAmount" else formattedAmount
    }

    /**
     * Formatte un pourcentage
     */
    fun formatPercentage(value: Float): String {
        return "%.1f%%".format(value)
    }

    /**
     * Formatte une date au format court
     */
    fun formatShortDate(date: Date): String {
        return shortDateFormat.format(date)
    }

    /**
     * Formatte une date au format long
     */
    fun formatLongDate(date: Date): String {
        return longDateFormat.format(date)
    }

    /**
     * Formatte une date au format mois/année
     */
    fun formatMonthYear(date: Date): String {
        return monthYearFormat.format(date)
    }

    /**
     * Formatte une durée en jours de manière lisible
     */
    fun formatDuration(days: Int): String {
        return when {
            days < 0 -> "Expiré"
            days == 0 -> "Aujourd'hui"
            days == 1 -> "1 jour"
            days < 7 -> "$days jours"
            days == 7 -> "1 semaine"
            days < 30 -> "${days / 7} semaines"
            days == 30 -> "1 mois"
            days < 365 -> "${days / 30} mois"
            days == 365 -> "1 an"
            else -> "${days / 365} ans"
        }
    }

    /**
     * Formatte un nombre avec séparateur de milliers
     */
    fun formatNumber(number: Number): String {
        return NumberFormat.getNumberInstance().format(number)
    }

    /**
     * Formatte une période en texte
     */
    fun formatPeriod(days: Int): String {
        return when (days) {
            1 -> "Journalier"
            7 -> "Hebdomadaire"
            30 -> "Mensuel"
            365 -> "Annuel"
            else -> "Tous les $days jours"
        }
    }

    /**
     * Parse une chaîne en montant
     * @return null si la chaîne n'est pas valide
     */
    fun parseAmount(text: String): Double? {
        return try {
            text.replace(",", ".")
                .replace(Regex("[^0-9.]"), "")
                .toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Parse une chaîne en date
     * @return null si la chaîne n'est pas valide
     */
    fun parseDate(text: String): Date? {
        return try {
            shortDateFormat.parse(text)
        } catch (e: Exception) {
            null
        }
    }
}
