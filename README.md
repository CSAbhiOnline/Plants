# Plant Identification App

This Android application allows users to take photos of plants and identify them using the Plant.id
API.

## Features

- Camera integration for taking plant photos
- Plant identification using the Plant.id API v3
- Display of plant name and description in a bottom sheet

## Setup

### API Key

To use this application, you need to obtain an API key from Plant.id:

1. Go to [Plant.id](https://web.plant.id/plant-identification-api/) and sign up for an account
2. After signing up, get your API key from the dashboard
3. Replace the placeholder API key in `PlantIdentificationService.kt`:

```kotlin
// Note: This is a test key. You should replace it with your own key from Plant.id
private val apiKey = "YOUR_API_KEY" 
```

### Building the App

1. Clone the repository
2. Open the project in Android Studio
3. Replace the API key as mentioned above
4. Build and run the application

## How to Use

1. Launch the app and grant camera permissions
2. Point the camera at a plant you want to identify
3. Press the capture button
4. Wait for the identification process to complete
5. View the plant name and description in the bottom sheet that appears

## Implementation Details

The implementation is focused on simplicity:

- Only extracts the plant name and description from the API response
- Uses Ktor for making API calls
- Uses Compose for UI including camera preview and bottom sheet