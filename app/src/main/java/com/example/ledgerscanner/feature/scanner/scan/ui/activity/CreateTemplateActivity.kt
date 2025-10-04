package com.example.ledgerscanner.feature.scanner.scan.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.image.ImageUtils
import com.example.ledgerscanner.feature.scanner.scan.model.OmrResult
import com.example.ledgerscanner.feature.scanner.scan.model.OmrTemplateResult
import com.example.ledgerscanner.feature.scanner.scan.ui.dialog.WarpedImageDialog
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor

class CreateTemplateActivity : BaseActivity() {

    companion object {
        const val PICK_IMAGE = "pick_image"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    content = { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            val navController = rememberNavController()
                            NavHost(navController, startDestination = PICK_IMAGE) {
                                composable(PICK_IMAGE) {
                                    PickImageScreen(navController)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun PickImageScreen(navController: NavHostController) {
        val context = LocalContext.current
        var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var templateResult by remember { mutableStateOf<OmrTemplateResult?>(null) }

        LaunchedEffect(templateResult) {
            templateResult?.let {
                if (!it.success && !it.reason.isNullOrEmpty()) {
                    Toast.makeText(context, it.reason, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val pickLauncher =
            createActivityLauncherComposeSpecific(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) {
                    return@createActivityLauncherComposeSpecific
                }
                pickedBitmap = ImageUtils.loadBitmapCorrectOrientation(
                    context,
                    uri,
                    reqWidth = 1080,
                    reqHeight = 1920
                )
            }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                templateResult?.let {
                    if (it.success && it.finalBitmap != null) {
                        Image(
                            bitmap = it.finalBitmap.asImageBitmap(),
                            contentDescription = "Captured image",
                            modifier = Modifier.Companion
                                .fillMaxSize()
                                .padding(16.dp),
                            filterQuality = FilterQuality.High
                        )
                    } else {
                        WarpedImageDialog(
                            warpedBitmap = it.finalBitmap,
                            intermediateBitmaps = it.debugBitmaps,
                            onDismiss = { templateResult = null },
                        )
                    }
                } ?: run {
                    pickedBitmap?.asImageBitmap()?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Captured image",
                            modifier = Modifier.Companion
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Companion.Fit
                        )
                    } ?: run {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .alpha(0.4f)
                                .padding(32.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Grey200)
                                .clickable {
                                    pickLauncher.launch("image/*")
                                }
                                .padding(horizontal = 16.dp, vertical = 50.dp)
                        ) {
                            Text(
                                "Pick Image from Gallery",
                                modifier = Modifier
                                    .align(Alignment.Center),
                                style = MaterialTheme.typography.labelMedium,
                                color = Grey500
                            )
                        }
                    }
                }
            }


            GenericButton(
                text = if (templateResult != null) "Reselect another Image" else "Process template",
                enabled = pickedBitmap != null,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = {
                    if (templateResult != null) {
                        templateResult = null
                        pickedBitmap = null
                    } else {
                        templateResult = TemplateProcessor().generateTemplateJson(pickedBitmap!!)
                    }
                }
            )
        }

    }
}