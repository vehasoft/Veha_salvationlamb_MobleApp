package com.veha.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

class WebViewActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        webView = findViewById(R.id.webView)
        val page: String = intent.getStringExtra("WebPageName").toString()
        val url: String = if (page.contentEquals("terms")){
            "https://salvationlamb.com/terms"
        } else {
            "https://salvationlamb.com/privacy"
        }

        webView.webViewClient = WebViewClient()

        // this will load the url of the website
        webView.loadUrl(url)

        // this will enable the javascript settings, it can also allow xss vulnerabilities
        webView.settings.javaScriptEnabled = true

        webView.settings.domStorageEnabled = true;

        // if you want to enable zoom feature
        //webView.settings.setSupportZoom(true)
    }
}