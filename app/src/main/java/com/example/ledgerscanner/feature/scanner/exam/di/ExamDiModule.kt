package com.example.ledgerscanner.feature.scanner.exam.di

import android.content.Context
import com.example.ledgerscanner.feature.scanner.exam.repo.TemplateSelectionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class ExamDiModule {

    @Provides
    @ViewModelScoped
    fun provideTemplateSelectionRepository(@ApplicationContext context: Context)
            : TemplateSelectionRepository =
        TemplateSelectionRepository(context)
}