package com.example.fbproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        val page: String = intent.getStringExtra("WebPageName").toString()
        var url: String
        if (page.contentEquals("terms")){
            url = "https://salvationlamb.com/terms"
        } else {
            url = "https://salvationlamb.com/privacy"
        }

        webView.webViewClient = WebViewClient()

        // this will load the url of the website
        webView.loadUrl(url)

        // this will enable the javascript settings, it can also allow xss vulnerabilities
        webView.settings.javaScriptEnabled = true

        // if you want to enable zoom feature
        webView.settings.setSupportZoom(true)
    }
}