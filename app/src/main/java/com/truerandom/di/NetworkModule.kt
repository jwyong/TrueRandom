package com.truerandom.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.truerandom.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies, such as OkHttpClient and Gson.
 *
 * This installs the dependencies at the Application level (SingletonComponent) so a single
 * instance of the client is used throughout the entire application.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides a singleton instance of Gson for JSON serialization/deserialization.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        // Use GsonBuilder for flexibility, though a simple Gson() works for now.
        return GsonBuilder()
            .setPrettyPrinting()
            .create()
    }

    /**
     * Provides a singleton instance of OkHttpClient.
     * Sets timeouts and configures the client for use in network requests.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // Configure timeouts to prevent indefinite waiting
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_PROJECT_URL,
            supabaseKey = BuildConfig.SUPABASE_API_KEY
        ) {
            install(Postgrest)
        }
    }
}
