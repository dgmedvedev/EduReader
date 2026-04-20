package com.example.edureader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.edureader.presentation.reader.ReaderRoute
import com.example.edureader.ui.theme.EduReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EduReaderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ReaderRoute(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
