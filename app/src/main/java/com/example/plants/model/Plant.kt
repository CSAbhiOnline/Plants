package com.example.plants.model

data class Plant(
    val name: String,
    val commonNames: List<String>? = null,
    val description: String? = null,
    val probability: Double
)