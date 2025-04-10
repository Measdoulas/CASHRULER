package com.cashruler.ui.screens.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cashruler.R
import com.cashruler.ui.components.*
import com.cashruler.ui.viewmodels.BackupViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedBackupPath by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }

    BaseScreen(
        title = stringResource(R.string.backup_restore),
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Actions principales
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.createBackup() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Créer une sauvegarde")
                    }

                    OutlinedButton(
                        onClick = { viewModel.importBackup() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importer depuis un fichier")
                    }
                }
            }

            // Liste des sauvegardes
            Text(
                text = "Sauvegardes disponibles",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn {
                items(viewModel.backups) { backup ->
                    BackupItem(
                        backup = backup,
                        onRestore = {
                            selectedBackupPath = backup.file.absolutePath
                            showRestoreConfirmation = true
                        },
                        onDelete = {
                            selectedBackupPath = backup.file.absolutePath
                            showDeleteConfirmation = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation && selectedBackupPath != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                selectedBackupPath = null
            },
            title = { Text("Supprimer la sauvegarde ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBackupPath?.let { viewModel.deleteBackup(it) }
                        showDeleteConfirmation = false
                        selectedBackupPath = null
                    }
                ) {
                    Text(
                        "Supprimer",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        selectedBackupPath = null
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showRestoreConfirmation && selectedBackupPath != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmation = false
                selectedBackupPath = null
            },
            title = { Text("Restaurer la sauvegarde ?") },
            text = { Text("Cette action remplacera toutes vos données actuelles. Voulez-vous continuer ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBackupPath?.let { viewModel.restoreBackup(it) }
                        showRestoreConfirmation = false
                        selectedBackupPath = null
                    }
                ) {
                    Text("Restaurer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmation = false
                        selectedBackupPath = null
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupItem(
    backup: BackupRepository.BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ListItem(
            headlineContent = {
                Text("Sauvegarde du ${dateFormat.format(backup.date)}")
            },
            supportingContent = {
                Text("Version ${backup.version}")
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onRestore) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restaurer"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }
}
