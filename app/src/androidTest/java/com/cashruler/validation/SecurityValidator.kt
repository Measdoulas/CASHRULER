package com.cashruler.validation

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Validateur de sécurité pour vérifier les aspects critiques de l'application
 */
class SecurityValidator {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val results = mutableListOf<SecurityResult>()

    /**
     * Effectue une validation complète de la sécurité
     */
    fun validateSecurity(): List<SecurityResult> {
        results.clear()

        // Vérification des permissions
        validatePermissions()

        // Vérification du stockage des données
        validateDataStorage()

        // Vérification de la configuration de sécurité
        validateSecurityConfig()

        // Vérification du chiffrement
        validateEncryption()

        // Vérification des sauvegardes
        validateBackupSecurity()

        return results
    }

    private fun validatePermissions() {
        val declaredPermissions = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions ?: emptyArray()

        // Vérifier les permissions dangereuses
        val dangerousPermissions = declaredPermissions.filter {
            try {
                val permissionInfo = context.packageManager.getPermissionInfo(it, 0)
                permissionInfo.protectionLevel and PackageManager.PERMISSION_GRANTED != 0
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        results.add(
            SecurityResult(
                "Permissions",
                dangerousPermissions.isEmpty(),
                "Permissions dangereuses détectées: ${dangerousPermissions.joinToString()}"
            )
        )
    }

    private fun validateDataStorage() {
        // Vérifier le stockage interne
        val internalDir = context.filesDir
        val files = internalDir.listFiles() ?: emptyArray()
        
        // Vérifier les fichiers sensibles
        val sensitiveFiles = files.filter {
            it.name.contains("password", ignoreCase = true) ||
            it.name.contains("key", ignoreCase = true) ||
            it.name.contains("secret", ignoreCase = true)
        }

        results.add(
            SecurityResult(
                "Data_Storage",
                sensitiveFiles.isEmpty(),
                if (sensitiveFiles.isNotEmpty()) {
                    "Fichiers sensibles détectés: ${sensitiveFiles.joinToString { it.name }}"
                } else {
                    "Aucun fichier sensible détecté"
                }
            )
        )

        // Vérifier les permissions des fichiers
        files.forEach { file ->
            validateFilePermissions(file)
        }
    }

    private fun validateFilePermissions(file: File) {
        val isReadableByOthers = file.canRead() && !file.setReadable(false, true)
        val isWritableByOthers = file.canWrite() && !file.setWritable(false, true)

        if (isReadableByOthers || isWritableByOthers) {
            results.add(
                SecurityResult(
                    "File_Permissions",
                    false,
                    "Permissions de fichier non sécurisées: ${file.name}"
                )
            )
        }
    }

    private fun validateSecurityConfig() {
        // Vérifier la version de l'API Android
        val isSecureApiLevel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        results.add(
            SecurityResult(
                "API_Level",
                isSecureApiLevel,
                if (isSecureApiLevel) {
                    "Version API sécurisée"
                } else {
                    "Version API non sécurisée: ${Build.VERSION.SDK_INT}"
                }
            )
        )

        // Vérifier le mode debuggable
        val isDebuggable = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        results.add(
            SecurityResult(
                "Debug_Mode",
                !isDebuggable,
                if (isDebuggable) {
                    "Mode debug activé - À désactiver en production"
                } else {
                    "Mode debug désactivé"
                }
            )
        )
    }

    private fun validateEncryption() {
        try {
            // Vérifier la disponibilité des algorithmes de chiffrement
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keyGen = MessageDigest.getInstance("SHA-256")
            
            results.add(
                SecurityResult(
                    "Encryption",
                    true,
                    "Algorithmes de chiffrement disponibles"
                )
            )
        } catch (e: Exception) {
            results.add(
                SecurityResult(
                    "Encryption",
                    false,
                    "Erreur de chiffrement: ${e.message}"
                )
            )
        }

        // Vérifier la force de la clé de chiffrement
        validateEncryptionStrength()
    }

    private fun validateEncryptionStrength() {
        try {
            val keySize = Cipher.getMaxAllowedKeyLength("AES")
            val isStrongEncryption = keySize >= 256

            results.add(
                SecurityResult(
                    "Encryption_Strength",
                    isStrongEncryption,
                    if (isStrongEncryption) {
                        "Chiffrement fort disponible (${keySize} bits)"
                    } else {
                        "Chiffrement faible détecté (${keySize} bits)"
                    }
                )
            )
        } catch (e: Exception) {
            results.add(
                SecurityResult(
                    "Encryption_Strength",
                    false,
                    "Erreur lors de la vérification de la force du chiffrement: ${e.message}"
                )
            )
        }
    }

    private fun validateBackupSecurity() {
        // Vérifier la configuration des sauvegardes
        val backupManager = context.getSystemService(Context.BACKUP_SERVICE)
        val allowsBackup = context.applicationInfo.flags and 
                          android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0

        results.add(
            SecurityResult(
                "Backup_Security",
                !allowsBackup,
                if (allowsBackup) {
                    "Les sauvegardes automatiques sont activées - Vérifier la sécurité"
                } else {
                    "Sauvegardes automatiques désactivées"
                }
            )
        )

        // Vérifier le dossier de sauvegarde
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (backupDir.exists()) {
            validateFilePermissions(backupDir)
        }
    }

    data class SecurityResult(
        val category: String,
        val isSecure: Boolean,
        val message: String
    )

    companion object {
        private const val MIN_ENCRYPTION_STRENGTH = 256
        private const val SECURE_API_LEVEL = Build.VERSION_CODES.M
    }
}
