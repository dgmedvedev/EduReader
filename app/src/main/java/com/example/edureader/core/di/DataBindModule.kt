package com.example.edureader.core.di

import com.example.edureader.data.local.BookCatalogLocalDataSource
import com.example.edureader.data.local.ReadingProgressLocalDataSource
import com.example.edureader.data.local.SharedPrefsBookCatalogLocalDataSource
import com.example.edureader.data.local.SharedPrefsReadingProgressLocalDataSource
import com.example.edureader.data.parser.EpubParser
import com.example.edureader.data.parser.ZipEpubParser
import com.example.edureader.data.repository.EpubRepositoryImpl
import com.example.edureader.data.repository.ReadingProgressRepositoryImpl
import com.example.edureader.data.source.AndroidEpubImportSourceResolver
import com.example.edureader.domain.repository.EpubRepository
import com.example.edureader.domain.repository.ReadingProgressRepository
import com.example.edureader.domain.service.EpubImportSourceResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindModule {

    @Binds
    @Singleton
    abstract fun bindEpubParser(
        impl: ZipEpubParser
    ): EpubParser

    @Binds
    @Singleton
    abstract fun bindBookCatalogLocalDataSource(
        impl: SharedPrefsBookCatalogLocalDataSource
    ): BookCatalogLocalDataSource

    @Binds
    @Singleton
    abstract fun bindReadingProgressLocalDataSource(
        impl: SharedPrefsReadingProgressLocalDataSource
    ): ReadingProgressLocalDataSource

    @Binds
    @Singleton
    abstract fun bindEpubRepository(
        impl: EpubRepositoryImpl
    ): EpubRepository

    @Binds
    @Singleton
    abstract fun bindReadingProgressRepository(
        impl: ReadingProgressRepositoryImpl
    ): ReadingProgressRepository

    @Binds
    @Singleton
    abstract fun bindEpubImportSourceResolver(
        impl: AndroidEpubImportSourceResolver
    ): EpubImportSourceResolver
}
