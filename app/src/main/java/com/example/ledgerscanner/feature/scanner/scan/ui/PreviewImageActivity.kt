package com.example.ledgerscanner.feature.scanner.scan.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.extensions.loadJsonFromAssets
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.utils.ImageUtils
import com.example.ledgerscanner.base.utils.TemplateProcessor
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.dialog.WarpedImageDialog
import com.example.ledgerscanner.feature.scanner.scan.utils.Try
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File

class PreviewImageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUri: Uri? = intent.getParcelableExtra("image_uri") ?: run {
            finish(); return
        }


        setContent {
            LedgerScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PreviewImageScreen(imageUri = imageUri!!, onClose = { finish() })
                }
            }
        }
    }

    @Composable
    fun PreviewImageScreen(imageUri: Uri, onClose: () -> Unit, onSubmit: (File) -> Unit = {}) {

        var showFinalProcessedImageDialog by remember { mutableStateOf(false) }
        var preProcessImage by remember { mutableStateOf<PreprocessResult?>(null) }

        val context = LocalContext.current
        // decode once on background thread and remember
        val bitmap by produceState<Bitmap?>(initialValue = null, imageUri) {
            value =
                ImageUtils.loadBitmapCorrectOrientation(
                    context,
                    imageUri,
                    reqWidth = 1080,
                    reqHeight = 1920
                )
        }

        val coroutineScope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {

            if (showFinalProcessedImageDialog && (preProcessImage?.warpedBitmap != null || preProcessImage?.intermediate?.isNotEmpty() == true)) {
                WarpedImageDialog(
                    warpedBitmap = preProcessImage?.warpedBitmap,
                    intermediateBitmaps = preProcessImage?.intermediate,
                    onDismiss = { showFinalProcessedImageDialog = false },
                    onRetry = {
                        showFinalProcessedImageDialog = false
                    },
                    onSave = {
                        showFinalProcessedImageDialog = false
                    }
                )
            } else {
                Column {
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

                    Box(
                        modifier = Modifier
                            .background(color = Grey200)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GenericButton(
                                text = "Rescan",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onClose()
                                }
                            )

                            bitmap?.let { bm ->
                                GenericButton(
                                    text = "Submit",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        coroutineScope.launch {
                                            val omrTemplate =
                                                context.loadJsonFromAssets<Template>("omr_template_1.json")
                                            if (omrTemplate == null) {
                                                Toast.makeText(context, "", Toast.LENGTH_SHORT)
                                                    .show()
                                            } else {
//                                                preProcessImage = TemplateProcessor.processWithTemplate(
//                                                    context,
//                                                    bm,
//                                                    omrTemplate,
//                                                    bm
//                                                )
//                                                showFinalProcessedImageDialog = true
                                                preProcessImage = Try.processOMRWithTemplate(bm, omrTemplate)
                                                showFinalProcessedImageDialog = true
                                            }
//                                            preProcessImage = OmrDetector.detectFilledBubbles(bm, true)
//                                            showFinalProcessedImageDialog = true

//                                            val result = debugPreprocessFile(bm, context, true)
//                                            preProcessImage = result
//                                            showFinalProcessedImageDialog = true
//                                            if (result.ok) {
//                                                preProcessImage = result
//                                                showFinalProcessedImageDialog = true
//                                            } else {
//                                                result.reason?.let {
//                                                    Toast.makeText(
//                                                        context, it,
//                                                        Toast.LENGTH_SHORT
//                                                    ).show()
//                                                } ?: run {
//                                                    Toast.makeText(
//                                                        context, "Not able to process the image",
//                                                        Toast.LENGTH_SHORT
//                                                    ).show()
//                                                }
//                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}