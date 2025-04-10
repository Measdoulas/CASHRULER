package com.cashruler.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * État de base pour un écran
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(
    title: String,
    modifier: Modifier = Modifier,
    navigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = if (navigateBack != null) {
                    {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour"
                            )
                        }
                    }
                } else null,
                actions = actions
            )
        },
        floatingActionButton = floatingActionButton,
        modifier = modifier
    ) { paddingValues ->
        content(paddingValues)
    }
}

/**
 * Affiche un état de chargement
 */
@Composable
fun LoadingState(
    message: String = "Chargement en cours...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Affiche un état d'erreur
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Text("Réessayer")
                }
            }
        }
    }
}

/**
 * Snackbar pour les messages de succès/erreur
 */
@Composable
fun MessageSnackbar(
    message: String,
    type: MessageType = MessageType.INFO,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val snackbarColor = when (type) {
        MessageType.SUCCESS -> MaterialTheme.colorScheme.primary
        MessageType.ERROR -> MaterialTheme.colorScheme.error
        MessageType.INFO -> MaterialTheme.colorScheme.secondary
    }

    Snackbar(
        modifier = Modifier.padding(16.dp),
        action = actionLabel?.let {
            {
                TextButton(
                    onClick = { onAction?.invoke() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(it)
                }
            }
        },
        dismissAction = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = snackbarColor,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(message)
    }
}

/**
 * Conteneur pour les écrans de formulaire
 */
@Composable
fun FormScreenContainer(
    title: String,
    onNavigateBack: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    submitEnabled: Boolean = true,
    submitLabel: String = "Valider",
    content: @Composable (PaddingValues) -> Unit
) {
    BaseScreen(
        title = title,
        navigateBack = onNavigateBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onSubmit,
                enabled = submitEnabled,
                text = { Text(submitLabel) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        content(paddingValues)
    }
}

enum class MessageType {
    SUCCESS,
    ERROR,
    INFO
}

@Preview(showBackground = true)
@Composable
fun ScreenComponentsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LoadingState(
                modifier = Modifier.height(200.dp)
            )

            ErrorState(
                message = "Une erreur est survenue",
                onRetry = {},
                modifier = Modifier.height(200.dp)
            )

            MessageSnackbar(
                message = "Opération réussie",
                type = MessageType.SUCCESS,
                onDismiss = {},
                actionLabel = "Annuler",
                onAction = {}
            )
        }
    }
}
