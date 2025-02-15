// ZoneProgressTest.kt
package com.bitfarmsoftware.sweatlog.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the extracted function [zoneProgress].
 *
 * We test three scenarios for each WorkoutZone:
 * - Lower region (raw progress ~0.05): For zones with a previous zone, we expect the downward
 *   ratio (i.e. 0.5 for 5% raw progress). For the first zone, since no previous zone exists, we
 *   expect 0.0.
 * - Middle region (raw progress ~0.50): Always returns 0.5.
 * - Upper region (raw progress ~0.95): For zones with a next zone, we expect the upward ratio
 *   (≈ 0.0556 for 95% raw progress). For the last zone, we expect 0.0.
 */
class ZoneProgressTest {

    @Test
    fun testZoneProgressLowerRegion() {
        workoutZones.forEach { zone ->
            val zoneRange = zone.max - zone.min
            // Choose a value 5% into the zone.
            val testValue = zone.min + 0.05f * zoneRange
            val result = zoneProgress(testValue.toString(), zone)
            if (zone.zone == workoutZones.first().zone) {
                // First zone has no previous zone – expect 0.0.
                assertEquals(
                    "Zone ${zone.zone} (lower region): Expected 0.0 when no previous zone exists",
                    0.0f,
                    result,
                    0.001f
                )
            } else {
                // For zones with a previous zone, the expected downward ratio is:
                // (0.10 - 0.05) / 0.10 = 0.5.
                assertEquals(
                    "Zone ${zone.zone} (lower region): Expected downward ratio 0.5",
                    0.5f,
                    result,
                    0.001f
                )
            }
        }
    }

    @Test
    fun testZoneProgressMiddleRegion() {
        workoutZones.forEach { zone ->
            val zoneRange = zone.max - zone.min
            // Choose a value 50% into the zone.
            val testValue = zone.min + 0.50f * zoneRange
            val result = zoneProgress(testValue.toString(), zone)
            // In the middle region, the function always returns 0.5.
            assertEquals(
                "Zone ${zone.zone} (middle region): Expected 0.5",
                0.5f,
                result,
                0.001f
            )
        }
    }

    @Test
    fun testZoneProgressUpperRegion() {
        workoutZones.forEach { zone ->
            val zoneRange = zone.max - zone.min
            // Choose a value 95% into the zone.
            val testValue = zone.min + 0.95f * zoneRange
            val result = zoneProgress(testValue.toString(), zone)
            if (zone.zone == workoutZones.last().zone) {
                // Last zone has no next zone – expect 0.0.
                assertEquals(
                    "Zone ${zone.zone} (upper region): Expected 0.0 when no next zone exists",
                    0.0f,
                    result,
                    0.001f
                )
            } else {
                // For zones with a next zone, the expected upward ratio is:
                // (0.95 - 0.90) / 0.90 ≈ 0.0556.
                val expected = (0.95f - 0.90f) / 0.90f
                assertEquals(
                    "Zone ${zone.zone} (upper region): Expected upward ratio $expected",
                    expected,
                    result,
                    0.001f
                )
            }
        }
    }
}