package com.example.edureader.core.di

import com.example.edureader.domain.repository.EpubRepository
import com.example.edureader.domain.repository.ReadingProgressRepository
import com.example.edureader.domain.usecase.GetBookUseCase
import com.example.edureader.domain.usecase.GetReadingProgressUseCase
import com.example.edureader.domain.usecase.ImportEpubFromUriUseCase
import com.example.edureader.domain.usecase.ImportLocalEpubUseCase
import com.example.edureader.domain.usecase.ObserveReadingProgressUseCase
import com.example.edureader.domain.usecase.ResolveInitialLocatorUseCase
import com.example.edureader.domain.usecase.SaveReadingProgressUseCase
import com.example.edureader.domain.service.EpubImportSourceResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideImportLocalEpubUseCase(
        epubRepository: EpubRepository
    ): ImportLocalEpubUseCase = ImportLocalEpubUseCase(epubRepository)

    @Provides
    @Singleton
    fun provideImportEpubFromUriUseCase(
        resolver: EpubImportSourceResolver,
        importLocalEpubUseCase: ImportLocalEpubUseCase
    ): ImportEpubFromUriUseCase = ImportEpubFromUriUseCase(resolver, importLocalEpubUseCase)

    @Provides
    @Singleton
    fun provideGetBookUseCase(
        epubRepository: EpubRepository
    ): GetBookUseCase = GetBookUseCase(epubRepository)

    @Provides
    @Singleton
    fun provideGetReadingProgressUseCase(
        repository: ReadingProgressRepository
    ): GetReadingProgressUseCase = GetReadingProgressUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveReadingProgressUseCase(
        repository: ReadingProgressRepository
    ): ObserveReadingProgressUseCase = ObserveReadingProgressUseCase(repository)

    @Provides
    @Singleton
    fun provideSaveReadingProgressUseCase(
        repository: ReadingProgressRepository
    ): SaveReadingProgressUseCase = SaveReadingProgressUseCase(repository)

    @Provides
    @Singleton
    fun provideResolveInitialLocatorUseCase(): ResolveInitialLocatorUseCase {
        return ResolveInitialLocatorUseCase()
    }
}
