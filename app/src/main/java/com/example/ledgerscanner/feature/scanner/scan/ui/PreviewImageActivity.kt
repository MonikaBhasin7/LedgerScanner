package com.example.ledgerscanner.feature.scanner.scan.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.utils.ImageUtils
import java.io.File

class PreviewImageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("image_path") ?: run {
            finish(); return
        }

        setContent {
            LedgerScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PreviewImageScreen(imagePath = path, onClose = { finish() })
                }
            }
        }
    }

    @Composable
    fun PreviewImageScreen(imagePath: String, onClose: () -> Unit, onSubmit: (File) -> Unit = {}) {

        // decode once on background thread and remember
        val bitmap by produceState<Bitmap?>(initialValue = null, imagePath) {
            // run in background automatically
            value =
                ImageUtils.loadCorrectlyOrientedBitmap(imagePath, reqWidth = 1080, reqHeight = 1920)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Image area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Captured image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } ?: run {
                    GenericLoader()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(modifier = Modifier.weight(1f), onClick = { onClose() }) {
                    Text("Rescan")
                }

                Button(modifier = Modifier.weight(1f), onClick = {
                    onSubmit(File(imagePath))
                }) {
                    Text("Submit")
                }
            }
        }
    }
}