package com.example.myrecordcollection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.myrecordcollection.ui.collection.CollectionRoute
import com.example.myrecordcollection.ui.theme.MyRecordCollectionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyRecordCollectionTheme {
                CollectionRoute(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
