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

package eu.europa.ec.commonfeature.delegate

import eu.europa.ec.commonfeature.interactor.ScopedPresentationInteractorDelegate
import eu.europa.ec.corelogic.controller.WalletCorePresentationController
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * The concrete subclasses of [eu.europa.ec.commonfeature.interactor.ScopedPresentationInteractorDelegate] live in feature modules
 * (PresentationSuccess, ProximitySuccess, PresentationRequest, ProximityRequest, etc.) so
 * Kover's per-module coverage of common-feature never exercises the delegate. These tests
 * drive coverage via a tiny in-test subclass.
 */
class TestScopedPresentationInteractorDelegate {

    @Mock
    private lateinit var walletCorePresentationController: WalletCorePresentationController

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun after() {
        closeable.close()
    }

    private class TestSubject(
        controller: WalletCorePresentationController? = null,
    ) : ScopedPresentationInteractorDelegate(controller) {
        fun exposedController(): WalletCorePresentationController = walletCorePresentationController
    }

    @Test
    fun `When constructed with a non-null controller, Then presentationScopeId is the default and the controller getter returns the injected instance`() {
        // When
        val subject = TestSubject(controller = walletCorePresentationController)

        // Then
        TestCase.assertEquals("DefaultPresentationScopeId", subject.presentationScopeId)
        TestCase.assertEquals(walletCorePresentationController, subject.exposedController())
    }

    @Test
    fun `When setScopeId is called, Then presentationScopeId is updated`() {
        // Given
        val subject = TestSubject(controller = walletCorePresentationController)
        val newScopeId = "newScope"

        // When
        subject.setScopeId(newScopeId)

        // Then
        TestCase.assertEquals(newScopeId, subject.presentationScopeId)
    }
}