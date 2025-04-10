package com.cashruler.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.*

/**
 * Élément de liste standard avec étiquette, description et montant
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListItem(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier,
    description: String? = null,
    date: Date? = null,
    icon: @Composable (() -> Unit)? = null,
    isPositiveGood: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = description?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = icon,
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedMoneyDisplay(
                    amount = amount,
                    isPositiveGood = isPositiveGood,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                trailing?.invoke()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    )
}

/**
 * Section de liste avec en-tête
 */
@Composable
fun ListSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            action?.invoke()
        }
        content()
    }
}

/**
 * Liste de transactions avec groupement par date
 */
@Composable
fun <T> TransactionList(
    items: List<T>,
    modifier: Modifier = Modifier,
    groupBy: (T) -> Date,
    key: ((T) -> Any)? = null,
    emptyContent: @Composable () -> Unit = { EmptyListMessage() },
    itemContent: @Composable (T) -> Unit
) {
    if (items.isEmpty()) {
        emptyContent()
    } else {
        LazyColumn(modifier = modifier) {
            val groupedItems = items.groupBy { groupBy(it) }
            groupedItems.forEach { (date, groupItems) ->
                item {
                    ListSection(
                        title = formatDate(date),
                        subtitle = "Total: ${groupItems.size} transactions"
                    ) {
                        Column {
                            groupItems.forEach { item ->
                                key?.let { keyFn -> 
                                    key(keyFn(item)) {
                                        itemContent(item)
                                    }
                                } ?: itemContent(item)
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Message affiché quand une liste est vide
 */
@Composable
fun EmptyListMessage(
    message: String = "Aucun élément à afficher",
    icon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
    }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon?.invoke()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Formatte une date (à remplacer par une vraie implémentation)
 */
private fun formatDate(date: Date): String {
    return date.toString()
}

@Preview(showBackground = true)
@Composable
fun ListComponentsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TransactionListItem(
                label = "Courses au supermarché",
                amount = -45.67,
                description = "Alimentation",
                icon = {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null
                    )
                }
            )
            
            Divider()

            TransactionListItem(
                label = "Salaire",
                amount = 2500.0,
                description = "Mensuel",
                icon = {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ListSection(
                title = "Aujourd'hui",
                subtitle = "Total: 3 transactions",
                action = {
                    TextButton(onClick = {}) {
                        Text("Voir tout")
                    }
                }
            ) {
                TransactionListItem(
                    label = "Restaurant",
                    amount = -25.80,
                    description = "Déjeuner"
                )
            }
        }
    }
}
