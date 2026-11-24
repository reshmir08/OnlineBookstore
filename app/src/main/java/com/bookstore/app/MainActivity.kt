package com.bookstore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstore.app.ui.navigation.AppRoot
import com.bookstore.app.ui.theme.BookstoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val session = (application as BookstoreApp).container.session
        setContent {
            BookstoreTheme {
                val userId by session.userId.collectAsStateWithLifecycle()
                AppRoot(isSignedIn = userId != null)
            }
        }
    }
}
