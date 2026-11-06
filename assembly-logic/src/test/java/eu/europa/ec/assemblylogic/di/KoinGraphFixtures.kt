/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.assemblylogic.di

import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.commonfeature.ui.biometric.BiometricViewModel
import eu.europa.ec.commonfeature.ui.pin.PinViewModel
import eu.europa.ec.commonfeature.ui.qr_scan.QrScanViewModel
import eu.europa.ec.commonfeature.ui.success.SuccessViewModel
import eu.europa.ec.commonfeature.ui.trustmark.TrustMarkViewModel
import eu.europa.ec.dashboardfeature.ui.documents.detail.DocumentDetailsViewModel
import eu.europa.ec.issuancefeature.ui.add.AddDocumentViewModel
import eu.europa.ec.issuancefeature.ui.code.DocumentOfferCodeViewModel
import eu.europa.ec.issuancefeature.ui.offer.DocumentOfferViewModel
import eu.europa.ec.issuancefeature.ui.success.DocumentIssuanceSuccessViewModel
import eu.europa.ec.presentationfeature.ui.loading.PresentationLoadingViewModel
import eu.europa.ec.presentationfeature.ui.request.PresentationRequestViewModel
import eu.europa.ec.presentationfeature.ui.success.PresentationSuccessViewModel
import eu.europa.ec.proximityfeature.ui.loading.ProximityLoadingViewModel
import eu.europa.ec.proximityfeature.ui.qr.ProximityQRViewModel
import eu.europa.ec.proximityfeature.ui.request.ProximityRequestViewModel
import eu.europa.ec.proximityfeature.ui.success.ProximitySuccessViewModel
import kotlin.reflect.KClass

private const val SERIALIZED_CONFIG = "serializedConfig"
private const val SCOPE_ID = "scopeId"
private const val DOCUMENT_ID = "documentId"
private const val TRANSACTION_ID = "transactionId"
private const val ACTION_TYPE = "actionType"

/**
 * The values each definition takes through `parametersOf`, in the order its navigation graph passes
 * them. Both graph tests read it: verification needs the types, resolution needs the values.
 */
internal val injectedParameterSamples: Map<KClass<*>, List<Any>> = mapOf(
    BiometricViewModel::class to listOf(SERIALIZED_CONFIG),
    PinViewModel::class to listOf(PinFlow.UPDATE),
    QrScanViewModel::class to listOf(SERIALIZED_CONFIG),
    SuccessViewModel::class to listOf(SERIALIZED_CONFIG),
    TrustMarkViewModel::class to listOf(SERIALIZED_CONFIG),
    DocumentDetailsViewModel::class to listOf(DOCUMENT_ID),
    dashboardClass("transactions.data_deletion.DataDeletionRequestViewModel") to
            listOf(TRANSACTION_ID),
    dashboardClass("transactions.detail.TransactionDetailsViewModel") to listOf(TRANSACTION_ID),
    dashboardClass("transactions.dpa_report.DpaReportViewModel") to listOf(TRANSACTION_ID),
    dashboardClass("transactions.history.TransactionHistoryViewModel") to
            listOf(TRANSACTION_ID, ACTION_TYPE),
    AddDocumentViewModel::class to listOf(SERIALIZED_CONFIG),
    DocumentOfferCodeViewModel::class to listOf(SERIALIZED_CONFIG),
    DocumentOfferViewModel::class to listOf(SERIALIZED_CONFIG),
    DocumentIssuanceSuccessViewModel::class to listOf(SERIALIZED_CONFIG),
    PresentationLoadingViewModel::class to listOf(SCOPE_ID),
    PresentationRequestViewModel::class to listOf(SERIALIZED_CONFIG),
    PresentationSuccessViewModel::class to listOf(SCOPE_ID),
    ProximityLoadingViewModel::class to listOf(SCOPE_ID),
    ProximityQRViewModel::class to listOf(SERIALIZED_CONFIG),
    ProximityRequestViewModel::class to listOf(SCOPE_ID),
    ProximitySuccessViewModel::class to listOf(SCOPE_ID),
)

// These ViewModels are internal to dashboard-feature, so they are looked up by name.
private fun dashboardClass(name: String): KClass<*> =
    Class.forName("eu.europa.ec.dashboardfeature.ui.$name").kotlin
