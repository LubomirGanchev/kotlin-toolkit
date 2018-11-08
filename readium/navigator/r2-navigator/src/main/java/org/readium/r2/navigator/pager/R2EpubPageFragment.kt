/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import org.json.JSONObject
import org.readium.r2.navigator.APPEARANCE_REF
import org.readium.r2.navigator.R
import org.readium.r2.navigator.R2EpubActivity
import org.readium.r2.navigator.SCROLL_REF
import org.readium.r2.shared.Locations
import org.readium.r2.shared.PageProgressionDirection


class R2EpubPageFragment : Fragment() {

    private val resourceUrl: String?
        get() = arguments!!.getString("url")

    private val bookTitle: String?
        get() = arguments!!.getString("title")

    lateinit var webView: R2WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_page_epub, container, false)
        val preferences = activity?.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)!!

        // Set text color depending of appearance preference
        (v.findViewById(R.id.book_title) as TextView).setTextColor(Color.parseColor(
                if (preferences.getInt(APPEARANCE_REF, 0) > 1) "#ffffff" else "#000000"
        ))

        val scrollMode = preferences.getBoolean(SCROLL_REF, false)
        when (scrollMode) {
            true -> {
                (v.findViewById(R.id.book_title) as TextView).visibility = View.GONE
                v.setPadding(0, 4, 0, 4)
            }
            false -> {
                (v.findViewById(R.id.book_title) as TextView).visibility = View.VISIBLE
                v.setPadding(0, 30, 0, 30)
            }
        }

        (v.findViewById(R.id.book_title) as TextView).text = bookTitle

        webView = v!!.findViewById(R.id.webView) as R2WebView

        webView.activity = activity as R2EpubActivity

        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.setPadding(0, 0, 0, 0)
        webView.addJavascriptInterface(webView, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.loadUrl(request.url.toString())
                return false
            }

            override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
                // Do something with the event here
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val currentFragment:R2EpubPageFragment = (webView.activity.resourcePager.adapter as R2PagerAdapter).getCurrentFragment() as R2EpubPageFragment
                val previousFragment:R2EpubPageFragment? = (webView.activity.resourcePager.adapter as R2PagerAdapter).getPreviousFragment() as? R2EpubPageFragment
                val nextFragment:R2EpubPageFragment? = (webView.activity.resourcePager.adapter as R2PagerAdapter).getNextFragment() as? R2EpubPageFragment

                if (this@R2EpubPageFragment.tag == currentFragment.tag) {
                    val locations = Locations.fromJSON(JSONObject(preferences.getString("${webView.activity.publicationIdentifier}-documentLocations", "{}")))
                    if (locations.id == null) {
                        locations.progression?.let { progression ->
                            currentFragment.webView.scrollToPosition(progression)
                        }
                    }
                }

                nextFragment?.let {
                    if (this@R2EpubPageFragment.tag == nextFragment.tag){
                        if (nextFragment.webView.activity.publication.metadata.direction == PageProgressionDirection.rtl.name) {
                            // The view has RTL layout
                            nextFragment.webView.scrollToPosition(1.0)
                        } else {
                            // The view has LTR layout
                            nextFragment.webView.scrollToPosition(0.0)
                        }
                    }
                }

                previousFragment?.let {
                    if (this@R2EpubPageFragment.tag == previousFragment.tag){
                        if (previousFragment.webView.activity.publication.metadata.direction == PageProgressionDirection.rtl.name) {
                            // The view has RTL layout
                            previousFragment.webView.scrollToPosition(0.0)
                        } else {
                            // The view has LTR layout
                            previousFragment.webView.scrollToPosition(1.0)
                        }
                    }
                }

            }

            // prevent favicon.ico to be loaded, this was causing NullPointerException in NanoHttp
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!request.isForMainFrame && request.url.path.endsWith("/favicon.ico")) {
                    try {
                        return WebResourceResponse("image/png", null, null)
                    } catch (e: Exception) {
                    }
                }
                return null
            }

        }
        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = false
        webView.setOnLongClickListener {
            true
        }

        val locations = Locations.fromJSON(JSONObject(preferences.getString("${webView.activity.publicationIdentifier}-documentLocations", "{}")))

        locations.id?.let {
            var anchor = it
            if (anchor.startsWith("#")) {
            } else {
                anchor = "#" + anchor
            }
            val href = resourceUrl +  anchor
            webView.loadUrl(href)
        }?:run {
            webView.loadUrl(resourceUrl)
        }


        return v
    }

    companion object {

        fun newInstance(url: String, title: String): R2EpubPageFragment {

            val args = Bundle()
            args.putString("url", url)
            args.putString("title", title)
            val fragment = R2EpubPageFragment()
            fragment.arguments = args
            return fragment
        }
    }

}


