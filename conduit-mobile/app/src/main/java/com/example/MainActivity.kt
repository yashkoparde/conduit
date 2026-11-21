package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.conduit.msme.ConduitApp
import com.conduit.msme.data.local.ConduitPreferencesDataStore
import com.conduit.msme.data.repository.ConduitRepository

class MainActivity : ComponentActivity() {

    private val repository: ConduitRepository by lazy {
        ConduitRepository(
            preferences = ConduitPreferencesDataStore(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConduitApp(repository = repository)
        }
    }
}
