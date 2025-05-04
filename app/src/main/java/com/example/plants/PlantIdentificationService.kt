package com.example.plants

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream

/**
 * Simple result model for plant identification
 */
data class PlantIdentificationResult(
    val success: Boolean,
    val name: String,
    val description: String
)

/**
 * Service for plant identification using Plant.id API
 */
class PlantIdentificationService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    // Note: This is a test key. You should replace it with your own key from Plant.id
    private val apiKey = "16B6Re0SlOFnSkgzcxPXFTchjjm8KUhHawZaKx0xWrxoVMyd10"

    /**
     * Identifies a plant from the provided image
     */
    suspend fun identifyPlant(bitmap: Bitmap): PlantIdentificationResult = withContext(Dispatchers.IO) {
        try {
            
            // Convert bitmap to base64
            val base64Image = bitmap.toBase64()
            Log.d("PlantIdentification", "Image converted to base64")
            
            // Prepare request body
            val requestBody = mapOf(
                "images" to listOf(base64Image)
            )
            Log.d("PlantIdentification", "Making API request to Plant.id")

            // Make API request
            val response: JsonObject = client.post("https://plant.id/api/v3/identification") {
                contentType(ContentType.Application.Json)
                header("Api-Key", apiKey)
                url {
                    parameters.append("details", "common_names,description")
                }
                setBody(requestBody)
            }.body()
            Log.d("PlantIdentification", "API response received")

            // Extract the information we need from the JSON response
            val result = response["result"]?.jsonObject
            val classification = result?.get("classification")?.jsonObject
            val suggestions = classification?.get("suggestions")?.jsonArray
            
            if (suggestions != null && suggestions.isNotEmpty()) {
                val suggestion = suggestions[0].jsonObject
                val name = suggestion["name"]?.jsonPrimitive?.content ?: "Unknown plant"
                
                // Try to extract description
                val details = suggestion["details"]?.jsonObject
                val descriptionObj = details?.get("description")?.jsonObject
                val description = descriptionObj?.get("value")?.jsonPrimitive?.content ?: "No description available"
                
                Log.d("PlantIdentification", "Plant identified: $name")
                PlantIdentificationResult(
                    success = true,
                    name = name,
                    description = description
                )
            } else {
                Log.d("PlantIdentification", "No suggestions found")
                PlantIdentificationResult(
                    success = false,
                    name = "Unknown plant",
                    description = "Could not identify the plant in the image"
                )
            }
        } catch (e: Exception) {
            Log.e("PlantIdentification", "Error identifying plant", e)
            e.printStackTrace()
            PlantIdentificationResult(
                success = false,
                name = "Error",
                description = "An error occurred: ${e.message}"
            )
        }
    }

    private fun Bitmap.toBase64(): String {
        // Scale down bitmap to reduce file size
        val maxWidth = 1000
        val maxHeight = 1000
        val scaleFactor = when {
            width > height && width > maxWidth -> maxWidth.toFloat() / width.toFloat()
            height > maxHeight -> maxHeight.toFloat() / height.toFloat()
            else -> 1.0f
        }
        
        val scaledBitmap = if (scaleFactor < 1.0f) {
            val newWidth = (width * scaleFactor).toInt()
            val newHeight = (height * scaleFactor).toInt()
            Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
        } else {
            this
        }
        
        // Compress to JPEG with reduced quality
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        
        // Log the size of the compressed image
        val imageSizeKb = byteArrayOutputStream.size() / 1024
        Log.d("PlantIdentification", "Compressed image size: $imageSizeKb KB")
        
        val byteArray = byteArrayOutputStream.toByteArray()
        
        // Clean up
        if (scaledBitmap != this) {
            scaledBitmap.recycle()
        }
        
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}