package com.example.data.model

data class RideInfo(
    val serviceName: String = "Uber", // Uber, Lyft, Waymo, Taxi, Private
    val driverName: String = "Alex Rivera",
    val vehicleModel: String = "Silver Toyota Camry",
    val licensePlate: String = "7XYZ892",
    val pickupAddress: String = "742 Evergreen Terrace",
    val destinationAddress: String = "Downtown Central Station",
    val isRideActive: Boolean = true,
    val startedAt: Long = System.currentTimeMillis()
)
