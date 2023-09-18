package eu.europa.ec.businesslogic.model

import com.google.gson.annotations.SerializedName

/**
 * Data class used to represent the Biometric information saved in shared preferences and are needed
 * to validate the biometric authentication.
 *
 * @param randomString          The random String that is used for biometric validation.
 * @param encryptedString       The random String encrypted with biometric cipher and in base64 form.
 * @param ivString              The iv used in biometric cipher in base64 form.
 */
data class BiometricData(
    @SerializedName("random") val randomString: String,
    @SerializedName("encrypted") val encryptedString: String,
    @SerializedName("iv") val ivString: String,
)