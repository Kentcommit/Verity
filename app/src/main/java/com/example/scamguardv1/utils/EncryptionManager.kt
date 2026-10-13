package com.example.scamguardv1.utils

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

/**
 * Manages AES-256-GCM encryption using Android Keystore for local security.
 * NOTE: For server-side KMS requirements, the file is encrypted locally and then 
 * handled by the server (e.g., Firebase Storage) which provides further at-rest encryption.
 */
class EncryptionManager(private val context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    /**
     * Encrypts a file using AES-256-GCM.
     */
    fun encryptFile(inputFile: File, outputFile: File) {
        val encryptedFile = EncryptedFile.Builder(
            outputFile,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { output ->
            inputFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Decrypts a file (for local verification if needed).
     */
    fun decryptFile(inputFile: File, outputFile: File) {
        val encryptedFile = EncryptedFile.Builder(
            inputFile,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileInput().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
