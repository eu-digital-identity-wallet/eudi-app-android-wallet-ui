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

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import eu.europa.ec.uilogic.component.IconDataUi
import eu.europa.ec.uilogic.util.MAX_REMOTE_IMAGE_BYTES
import eu.europa.ec.uilogic.util.remoteImageHttpClient
import eu.europa.ec.uilogic.util.remoteImageSourceOrNull

@Composable
fun WrapAsyncImage(
    source: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: IconDataUi? = null,
    error: IconDataUi? = null,
    fallback: IconDataUi? = null,
) {
    WrapAsyncImage(
        source = source,
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        fallback = fallback,
        onLoading = {},
        onSuccess = {},
        onError = {},
    )
}

/**
 * Shows the remote image at [source]. Only an `https` URL is loaded (see [remoteImageSourceOrNull]);
 * for anything else [fallback], or [error] when there is no fallback, is shown and [onError] called. A download stops once it exceeds
 * [MAX_REMOTE_IMAGE_BYTES]. Coil scales the decoded image to the space it is shown in, and to at
 * most 4096 pixels a side.
 */
@Composable
fun WrapAsyncImage(
    source: String,
    modifier: Modifier,
    contentDescription: String?,
    contentScale: ContentScale,
    placeholder: IconDataUi?,
    error: IconDataUi?,
    fallback: IconDataUi?,
    onLoading: () -> Unit,
    onSuccess: () -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                add(OkHttpNetworkFetcherFactory(callFactory = { remoteImageHttpClient }))
            }
            .build()
    }
    val request = remember(context, source) {
        ImageRequest.Builder(context).data(remoteImageSourceOrNull(source)).build()
    }

    DisposableEffect(imageLoader) {
        onDispose { imageLoader.shutdown() }
    }

    AsyncImage(
        modifier = modifier,
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        error = error?.resourceId?.let { resourceId -> painterResource(resourceId) },
        fallback = (fallback
            ?: error)?.resourceId?.let { resourceId -> painterResource(resourceId) },
        placeholder = placeholder?.resourceId?.let { resourceId -> painterResource(resourceId) },
        onLoading = { onLoading() },
        onSuccess = { onSuccess() },
        onError = { onError() },
    )
}