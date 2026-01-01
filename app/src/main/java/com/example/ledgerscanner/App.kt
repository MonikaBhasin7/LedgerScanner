package com.example.ledgerscanner

import android.app.Application
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("opencv_java4") // or opencv_java3 for older SDKs
        } catch (t: UnsatisfiedLinkError) {
        }
    }

}

object Temporary {
    var omrImageProcessResult : OmrImageProcessResult?= null
}