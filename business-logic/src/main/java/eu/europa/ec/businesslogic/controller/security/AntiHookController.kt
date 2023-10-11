/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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

class AntiHookControllerImpl constructor(
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
                        zygoteInitCallCount++;
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