package com.example.myapplication.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://data.montpellier3m.fr/"

    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .readTimeout(60, TimeUnit.SECONDS)  // Augmenter le timeout de lecture pour les grands téléchargements
            .connectTimeout(60, TimeUnit.SECONDS)  // Augmenter le timeout de connexion
            .followRedirects(true)  // S'assurer que le client suit les redirections
            .build()
    }

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            // Utiliser un convertisseur Factory approprié pour des réponses de type non JSON, si nécessaire
            .build()
            .create(ApiService::class.java)
    }
}
