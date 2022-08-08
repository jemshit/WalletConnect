/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample

import android.widget.ImageView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest


fun ImageView.loadSvgImage(url: String?) {
    if (url.isNullOrBlank()) {
        this.setImageDrawable(null)
        return
    }

    val imageLoader = ImageLoader.Builder(this.context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    val request = ImageRequest.Builder(this.context)
            .data(url)
            .target(this)
            .build()
    imageLoader.enqueue(request)
}