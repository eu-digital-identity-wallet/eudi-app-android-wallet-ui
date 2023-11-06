/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import eu.europa.ec.eudi.wallet.ui.databinding.ActivityMainBinding
import eu.europa.ec.eudi.wallet.ui.share.ShareViewModel
import eu.europa.ec.eudi.wallet.ui.transfer.TransferFragment
import eu.europa.ec.eudi.wallet.ui.util.log

class MainActivity : WalletUIBaseActivity() {

    private val viewModel: ShareViewModel by viewModels()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.appVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        appBarConfiguration = AppBarConfiguration
            .Builder(R.id.WalletFragment, R.id.ShareFragment, R.id.TransferFragment)
            .build()

        setupActionBarWithNavController(
            findNavController(R.id.nav_host_fragment),
            appBarConfiguration
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        log("New intent on Activity $intent")
        handleIntent(intent)
    }

    // To invoke the mdoc App through the mdoc scheme (mdoc://)
    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.data == null) return
        // clear intent after use
        setIntent(null)
        when (intent.scheme) {
            "mdoc" -> {
                viewModel.startEngagementToApp(intent)
                findNavController(R.id.nav_host_fragment).navigate(
                    R.id.TransferFragment,
                    bundleOf(TransferFragment.CLOSE_AFTER_RESPONSE_KEY to true)
                )
            }
            "mdoc-openid4vp", "https" -> { // openid4vp
                findNavController(R.id.nav_host_fragment).navigate(
                    R.id.TransferFragment,
                    bundleOf(
                        TransferFragment.OPENID4VP_URI_KEY to intent.toUri(0),
                        TransferFragment.CLOSE_AFTER_RESPONSE_KEY to true
                    )
                )
            }
        }
    }
}
