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

import android.os.Process
import eu.europa.ec.businesslogic.controller.log.LogController
import java.io.BufferedReader
import java.io.FileReader

interface AntiHookController {

    fun isStacktraceHooked(): Boolean

    fun isMemoryHooked(): Boolean
}

class AntiHookControllerImpl(
    private val logController: LogController
) : AntiHookController {

    override fun isStacktraceHooked(): Boolean {
        try {
            throw Exception()
        } catch (e: Exception) {
            var zygoteInitCallCount = 0
            e.stackTrace.forEach { element ->
                when {
                    "com.android.internal.os.ZygoteInit" == element.className -> {
                        zygoteInitCallCount++
                        if (zygoteInitCallCount == 2) {
                            logController.d(this.javaClass.simpleName) {
                                "Substrate is active on the device."
                            }
                            return true
                        }
                    }

                    "com.saurik.substrate.MS$2" == element.className &&
                            "invoked" == element.methodName -> {
                        logController.d(this.javaClass.simpleName) {
                            "A method on the stack trace has been hooked using Substrate."
                        }
                        return true
                    }

                    "de.robv.android.xposed.XposedBridge" == element.className &&
                            "main" == element.methodName -> {
                        logController.d(this.javaClass.simpleName) {
                            "Xposed is active on the device."
                        }
                        return true
                    }

                    "de.robv.android.xposed.XposedBridge" == element.className &&
                            "handleHookedMethod" == element.methodName -> {
                        logController.d(this.javaClass.simpleName) {
                            "A method on the stack trace has been hooked using Xposed."
                        }
                        return true
                    }
                }
            }
            return false
        }
    }

    override fun isMemoryHooked(): Boolean {
        try {
            val mapsFilename = "/proc/" + Process.myPid() + "/maps"
            BufferedReader(FileReader(mapsFilename)).use { reader ->
                val libraries = reader.readLines().filter { line ->
                    val n = line.lastIndexOf(" ")
                    val library = line.substring(n + 1)
                    (line.endsWith(".so") || line.endsWith(".jar")) &&
                            library.contains("com.saurik.substrate") ||
                            library.contains("XposedBridge.jar")
                }
                return libraries.isNotEmpty()
            }
        } catch (e: Exception) {
            logController.e(javaClass.simpleName, e)
            return false
        }
    }
}