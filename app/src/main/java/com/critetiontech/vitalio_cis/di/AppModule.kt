package com.critetiontech.vitalio_cis.di

import android.content.Context
import com.critetiontech.vitalio_cis.utils.PrefsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providePrefsManager(@ApplicationContext context: Context): PrefsManager = PrefsManager(context)
}
