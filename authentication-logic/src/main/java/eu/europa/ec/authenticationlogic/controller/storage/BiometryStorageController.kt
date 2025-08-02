/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.authenticationlogic.controller.storage

import eu.europa.ec.authenticationlogic.config.StorageConfig
import eu.europa.ec.authenticationlogic.model.BiometricAuthentication

interface BiometryStorageController {
    fun getBiometricAuthentication(): BiometricAuthentication?
    fun setBiometricAuthentication(value: BiometricAuthentication?)
    fun setUseBiometricsAuth(value: Boolean)
    fun getUseBiometricsAuth(): Boolean
}

class BiometryStorageControllerImpl(private val storageConfig: StorageConfig) :
    BiometryStorageController {
    override fun getBiometricAuthentication(): BiometricAuthentication? =
        storageConfig.biometryStorageProvider.getBiometricAuthentication()

    override fun setBiometricAuthentication(value: BiometricAuthentication?) {
        storageConfig.biometryStorageProvider.setBiometricAuthentication(value)
    }

    override fun setUseBiometricsAuth(value: Boolean) {
        storageConfig.biometryStorageProvider.setUseBiometricsAuth(value)
    }

    override fun getUseBiometricsAuth(): Boolean =
        storageConfig.biometryStorageProvider.getUseBiometricsAuth()
}