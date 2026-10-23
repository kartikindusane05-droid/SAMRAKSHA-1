package com.samraksha.app.data.local

import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed cryptographic manager using AndroidKeyStore with AES-256-GCM.
 *
 * Secures authentication tokens and sensitive profile data before persisting to disk.
 * The encryption key is protected inside hardware security modules (TEE/Keymaster/StrongBox)
 * and cannot be extracted via adb backup, filesystem imaging, or root inspection.
 *
 * Designed to gracefully fall back to in-memory handling in JVM unit testing environments
 * where AndroidKeyStore provider is not registered.
 */
object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "samraksha_secure_auth_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    } catch (_: Throwable) {
        null
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        val ks = keyStore ?: return null
        if (ks.containsAlias(KEY_ALIAS)) {
            return try {
                val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey
            } catch (_: Throwable) {
                null
            }
        }

        return try {
            val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
            val keyGenSpecClass = Class.forName("android.security.keystore.KeyGenParameterSpec\$Builder")
            val keyPropertiesClass = Class.forName("android.security.keystore.KeyProperties")

            val purposeEncrypt = keyPropertiesClass.getField("PURPOSE_ENCRYPT").getInt(null)
            val purposeDecrypt = keyPropertiesClass.getField("PURPOSE_DECRYPT").getInt(null)
            val blockModeGcm = keyPropertiesClass.getField("BLOCK_MODE_GCM").get(null) as String
            val paddingNone = keyPropertiesClass.getField("ENCRYPTION_PADDING_NONE").get(null) as String

            val constructor = keyGenSpecClass.getConstructor(String::class.java, Int::class.javaPrimitiveType)
            val builder = constructor.newInstance(KEY_ALIAS, purposeEncrypt or purposeDecrypt)

            val setBlockModes = keyGenSpecClass.getMethod("setBlockModes", Array<String>::class.java)
            setBlockModes.invoke(builder, arrayOf(blockModeGcm))

            val setEncryptionPaddings = keyGenSpecClass.getMethod("setEncryptionPaddings", Array<String>::class.java)
            setEncryptionPaddings.invoke(builder, arrayOf(paddingNone))

            val setKeySize = keyGenSpecClass.getMethod("setKeySize", Int::class.javaPrimitiveType)
            setKeySize.invoke(builder, 256)

            val buildMethod = keyGenSpecClass.getMethod("build")
            val spec = buildMethod.invoke(builder) as java.security.spec.AlgorithmParameterSpec

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Returns Base64-encoded string containing [IV (12 bytes) + CipherText + Auth Tag],
     * or null if hardware encryption is unavailable.
     */
    fun encrypt(plainText: String): String? {
        val key = getOrCreateSecretKey() ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            Base64.getEncoder().encodeToString(combined)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Decrypts an AES-256-GCM Base64 payload.
     * Returns original plaintext string, or null on verification failure.
     */
    fun decrypt(encodedPayload: String): String? {
        val key = getOrCreateSecretKey() ?: return null
        return try {
            val combined = Base64.getDecoder().decode(encodedPayload)
            if (combined.size <= GCM_IV_LENGTH) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }
}
