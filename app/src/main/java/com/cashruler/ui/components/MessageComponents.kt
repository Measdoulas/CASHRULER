package com.cashruler.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class MessageType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

@Composable
fun MessageSnackbar(
    message: String,
    type: MessageType,
    onDismiss: () -> Unit,
    duration: Int = 3000,
    modifier: Modifier = Modifier
) {
    val snackbarData = remember {
        object : SnackbarData {
            override val visuals = object : SnackbarVisuals {
                override val actionLabel: String? = null
                override val duration: SnackbarDuration = SnackbarDuration.Short
                override val message: String = message
                override val withDismissAction: Boolean = true
            }
            override fun dismiss() = onDismiss()
            override fun performAction() {}
        }
    }

    LaunchedEffect(message) {
        kotlinx.coroutines.delay(duration.toLong())
        onDismiss()
    }

    val backgroundColor = when (type) {
        MessageType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        MessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
        MessageType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        MessageType.INFO -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when (type) {
        MessageType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        MessageType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        MessageType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        MessageType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Snackbar(
            snackbarData = snackbarData,
            containerColor = backgroundColor,
            contentColor = contentColor,
            actionColor = contentColor,
            dismissActionContentColor = contentColor,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun LoadingState(
    message: String = "Chargement en cours...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(message)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageSnackbarPreview() {
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            MessageSnackbar(
                message = "Opération réussie",
                type = MessageType.SUCCESS,
                onDismiss = {}
            )
            MessageSnackbar(
                message = "Une erreur est survenue",
                type = MessageType.ERROR,
                onDismiss = {}
            )
            MessageSnackbar(
                message = "Attention",
                type = MessageType.WARNING,
                onDismiss = {}
            )
            MessageSnackbar(
                message = "Information",
                type = MessageType.INFO,
                onDismiss = {}
            )
        }
    }
}
