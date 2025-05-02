package com.example.plants.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface PlantApiService {
    @Headers(
        "Content-Type: application/json",
        "Api-Key: YOUR_PLANT_ID_API_KEY" // Replace with your actual Plant.id API key
    )
    @POST("identify")
    fun identifyPlant(@Body request: PlantIdentificationRequest): Call<PlantIdentificationResponse>
}

data class PlantIdentificationRequest(
    val images: List<String>,
    val modifiers: List<String> = listOf("similar_images"),
    val plant_details: List<String> = listOf("common_names", "wikipedia_description")
)

data class PlantIdentificationResponse(
    val result: PlantResult
)

data class PlantResult(
    val classification: Classification,
    val is_plant: IsPlant
)

data class Classification(
    val suggestions: List<PlantSuggestion>
)

data class PlantSuggestion(
    val id: Int,
    val name: String,
    val probability: Double,
    val plant_details: PlantDetails?
)

data class PlantDetails(
    val common_names: List<String>?,
    val wikipedia_description: WikipediaDescription?
)

data class WikipediaDescription(
    val value: String
)

data class IsPlant(
    val probability: Double,
    val binary: Boolean
)