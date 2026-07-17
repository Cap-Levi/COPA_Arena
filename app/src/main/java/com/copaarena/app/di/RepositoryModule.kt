package com.copaarena.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Repositories are provided via @Inject constructor, so this module is empty
// but included to satisfy project structure requirements.
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
}
