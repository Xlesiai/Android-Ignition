package com.example.ignition

import java.time.Instant
import kotlin.math.sqrt


data class Vector3D (
    var x: Float,
    var y: Float,
    var z: Float
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3D(x / scalar, y / scalar, z / scalar)

    fun normalize() {
        val length = magnitude()
        if (length > 0) {
            x /= length
            y /= length
            z /= length
        }
    }
    fun magnitude(): Float {
        return sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }

    override fun toString(): String {
        return "{\n" +
                "  \"timestamp\": ${Instant.now()},\n" +
                "  \"metrics\": [\n" +
                "    {\n" +
                "      \"name\": \"AccelerometerX\",\n" +
                "      \"value\": ${x}\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"AccelerometerY\",\n" +
                "      \"value\": ${y}\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"AccelerometerZ\",\n" +
                "      \"value\": ${z}\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
    }
}
