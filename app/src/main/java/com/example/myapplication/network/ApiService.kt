package com.example.myapplication.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("TAM_MMM_GTFSRT/GTFS.zip")
    fun downloadGTFSZip(): Call<ResponseBody>
}
