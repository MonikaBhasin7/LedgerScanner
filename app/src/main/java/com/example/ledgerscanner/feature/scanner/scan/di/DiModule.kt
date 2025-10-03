package com.example.ledgerscanner.feature.scanner.scan.di

import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
class ActivityDiModule {

    @Provides
    fun provideOmrProcessor(): OmrProcessor = OmrProcessor()

    @Provides
    fun provideTemplateProcessor(): TemplateProcessor = TemplateProcessor()
}