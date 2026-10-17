package win.haya.doko

import java.io.File

data class Coordinates(val lat: Double, val lon: Double)

data class RegisteredShop(
    val coordinates: Coordinates,
    val name: String,
    val prefecture: String
)

data class ShopInfo(
    val name: String,
    val coordinates: Coordinates,
    val distanceMeters: Float,
    val bearingDegrees: Double
)

data class PhotoEntry(
    val shopName: String,
    val file: File,
    val timestampMillis: Long
)
