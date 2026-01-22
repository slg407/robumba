package com.lxmf.messenger.ui.screens.rBrowser

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket
import java.util.ArrayList // Import standard ArrayList

object ServerState {
    var isStarted = false
    var isReady by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

@Composable
fun RBrowserScreen() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (!ServerState.isStarted) {
                ServerState.isStarted = true

                launch {
                    try {
                        if (!Python.isStarted()) {
                            Python.start(AndroidPlatform(context))
                        }
                        val py = Python.getInstance()
                        val sys = py.getModule("sys")
                        val builtins = py.getModule("builtins")

                        // FIXED: Use a standard ArrayList instead of Kotlin's listOf.
                        // "Arrays$ArrayList" (from listOf) causes the "not iterable" crash in Python.
                        val args = ("rBrowser.py")

                        // builtins.list() can definitely iterate a standard Java ArrayList
                        sys["argv"] = builtins.callAttr("list", args)

                        val rBrowser = py.getModule("rBrowser")
                        rBrowser.callAttr("main")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ServerState.error = "Python Crash: ${e.message}"
                        ServerState.isStarted = false
                    }
                }
            }

            var attempts = 0
            while (!ServerState.isReady && attempts < 30) {
                try {
                    val socket = Socket("127.0.0.1", 5000)
                    socket.close()
                    ServerState.isReady = true
                } catch (e: Exception) {
                    delay(500)
                    attempts++
                }
            }

            if (!ServerState.isReady && ServerState.error == null) {
                ServerState.error = "Server timed out."
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (ServerState.isReady) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                return false
                            }
                        }

                        loadUrl("http://127.0.0.1:5000")
                    }
                }
            )
        } else if (ServerState.error != null) {
            Text(text = "Error: ${ServerState.error}")
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Initializing Reticulum Browser...")
            }
        }
    }
}
