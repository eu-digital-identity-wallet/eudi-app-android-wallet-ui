/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.businesslogic.controller.security

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

enum class AndroidInstaller {
    TRUSTED, UNKNOWN
}

interface AndroidPackageController {
    fun getSignatures(): List<String>
    fun getInstaller(trusted: List<String>): AndroidInstaller
    fun isDebugModeEnabled(): Boolean
    fun getSmsHashCodes(): List<String>
}

class AndroidPackageControllerImpl constructor(
    private val resourceProvider: ResourceProvider,
    private val logController: LogController
) : AndroidPackageController {

    companion object {
        private const val HASH_TYPE = "SHA-256"
        private const val NUM_HASHED_BYTES = 9
        private const val NUM_BASE64_CHAR = 11
    }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    override fun getSmsHashCodes(): List<String> {
        try {

            val packageName: String = resourceProvider.provideContext().packageName
            val packageManager: PackageManager = resourceProvider.provideContext().packageManager
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            return packageInfo.signatures.mapNotNull { signature ->
                hash(packageName, signature.toCharsString())?.let {
                    logController.d { "Hash $it" }
                    it
                }
            }
        } catch (e: Exception) {
            logController.e(javaClass.simpleName, e)
        }
        return emptyList()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    override fun getSignatures(): List<String> {
        try {
            val packageInfo = resourceProvider.provideContext().packageManager.getPackageInfo(
                resourceProvider.provideContext().packageName,
                PackageManager.GET_SIGNATURES
            )
            return packageInfo.signatures.map {
                val md: MessageDigest = MessageDigest.getInstance("SHA").apply {
                    update(it.toByteArray())
                }
                Base64.encodeToString(md.digest(), Base64.DEFAULT).trim()
            }
        } catch (e: Exception) {
            logController.e(javaClass.simpleName, e)
        }
        return emptyList()
    }

    @Suppress("DEPRECATION")
    override fun getInstaller(trusted: List<String>): AndroidInstaller {
        val packageManager = resourceProvider.provideContext().packageManager
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(
                resourceProvider.provideContext().packageName
            ).installingPackageName
        } else {
            packageManager.getInstallerPackageName(
                resourceProvider.provideContext().packageName
            )
        }
        return installer?.let {
            if (trusted.contains(it)) {
                AndroidInstaller.TRUSTED
            } else {
                AndroidInstaller.UNKNOWN
            }
        } ?: AndroidInstaller.UNKNOWN
    }

    override fun isDebugModeEnabled(): Boolean {
        return 0 != resourceProvider.provideContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
    }

    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        try {
            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
            messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
            var hashSignature = messageDigest.digest()
            hashSignature = Arrays.copyOfRange(
                hashSignature,
                0,
                NUM_HASHED_BYTES
            )
            var base64Hash = Base64.encodeToString(
                hashSignature,
                Base64.NO_PADDING or Base64.NO_WRAP
            )
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR)
            logController.d { "pkg: $packageName -- hash: $base64Hash" }
            return base64Hash
        } catch (e: Exception) {
            logController.e(javaClass.simpleName, e)
        }
        return null
    }
}