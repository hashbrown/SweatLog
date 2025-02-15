package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.db.WorkoutData
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.Duration
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class WorkoutSessionTest {

    private lateinit var baseInstant: Instant
    private lateinit var testClock: MutableClock
    private lateinit var session: WorkoutSession

    @Before
    fun setup() {
        // Start with a fixed instant for the clock
        baseInstant = Instant.parse("2025-02-01T12:00:00Z")
        testClock = MutableClock(baseInstant)
        session = WorkoutSession(clock = testClock)
    }

    @Test
    fun `first WorkoutData record initializes WorkoutSegment`() {
        val data = WorkoutData(resistance = 5)

        session.accumulate(data)

        assertEquals(1, session.segments.size)
        assertEquals(5, session.segments.first().data.resistance)
    }

    @Test
    fun `no new WorkoutSegment when resistance has not stabilized for 10 seconds`() {
        val data1 = WorkoutData(resistance = 7, speed = 10.0)
        val data2 = WorkoutData(resistance = 7, speed = 20.0)

        session.accumulate(data1)
        testClock.advance(Duration.ofSeconds(9)) // Less than 10s threshold
        session.accumulate(data2)

        assertEquals(1, session.segments.size)
        assertEquals(7, session.segments.last().data.resistance)
        assertEquals(20.0, session.segments.last().data.speed)
    }

    @Test
    fun `no new WorkoutSegment when changing resistance has not stabilized for 10 seconds`() {
        val data1 = WorkoutData(resistance = 5, speed = 10.0)
        val data2 = WorkoutData(resistance = 7, speed = 20.0)
        val data3 = WorkoutData(resistance = 5, speed = 30.0)// Changes back too quickly

        session.accumulate(data1)

        assertEquals(1, session.segments.size)
        assertEquals(5, session.segments.last().data.resistance)
        assertEquals(10.0, session.segments.last().data.speed)

        testClock.advance(Duration.ofSeconds(2)) // Less than 10s threshold
        session.accumulate(data2)

        assertEquals(1, session.segments.size)
        assertEquals(7, session.segments.last().data.resistance)
        assertEquals(20.0, session.segments.last().data.speed)

        testClock.advance(Duration.ofSeconds(11)) // 10 seconds had passed but now a new value
        session.accumulate(data3)

        assertEquals(1, session.segments.size)
        assertEquals(5, session.segments.last().data.resistance)
        assertEquals(30.0, session.segments.last().data.speed)
    }


    @Test
    fun `new WorkoutSegment when resistance stabilizes after delay`() {
        val data1 = WorkoutData(resistance = 7, speed = 10.0)
        val data2 = WorkoutData(resistance = 7, speed = 20.0)
        val data3 = WorkoutData(resistance = 7, speed = 30.0)
        val data4 = WorkoutData(resistance = 8, speed = 40.0)

        session.accumulate(data1)

        assertEquals(1, session.segments.size)
        assertEquals(7, session.segments.last().data.resistance)
        assertEquals(10.0, session.segments.last().data.speed)

        testClock.advance(Duration.ofSeconds(4)) // Less than 10s threshold
        session.accumulate(data2)

        assertEquals(1, session.segments.size)
        assertEquals(7, session.segments.last().data.resistance)
        assertEquals(20.0, session.segments.last().data.speed)

        testClock.advance(Duration.ofSeconds(7)) // Advance past 10s threshold
        session.accumulate(data3) // not stable until after the accumulation

        assertEquals(1, session.segments.size)
        assertEquals(7, session.segments.last().data.resistance)
        assertEquals(30.0, session.segments.last().data.speed)

        session.accumulate(data4) // not stable until after the accumulation

        assertEquals(2, session.segments.size)
        assertEquals(8, session.segments.last().data.resistance)
        assertEquals(40.0, session.segments.last().data.speed)
    }


    @Test
    fun `new WorkoutSegment when MAX_SEGMENT_TIME is reached, even if resistance is unchanged`() {
        val data1 = WorkoutData(resistance = 5, speed = 10.0)
        val data2 = WorkoutData(resistance = 5, speed = 20.0) // Same resistance, but time will trigger a new segment

        session.accumulate(data1)
        testClock.advance(WorkoutSession.MAX_SEGMENT_TIME.plus(1.seconds).toJavaDuration()) // Advance past segment max time
        session.accumulate(data2)

        assertEquals(2, session.segments.size)
        assertEquals(5, session.segments.last().data.resistance) // Same resistance, but new segment
        assertEquals(20.0, session.segments.last().data.speed)
    }

    @Test
    fun `no new WorkoutSegment when resistance is unchanged and MAX_SEGMENT_TIME has not passed`() {
        val data1 = WorkoutData(resistance = 5, speed = 10.0)
        val data2 = WorkoutData(resistance = 5, speed = 20.0)

        session.accumulate(data1)
        testClock.advance(Duration.ofMinutes(14)) // Less than MAX_SEGMENT_TIME (15 min)

        session.accumulate(data2)

        assertEquals(1, session.segments.size)
        assertEquals(5, session.segments.last().data.resistance)
        assertEquals(20.0, session.segments.last().data.speed)
    }
}

/**
 * MutableClock class to simulate a controllable time source.
 */
class MutableClock(private var currentInstant: Instant) : Clock() {
    override fun getZone(): ZoneOffset = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = currentInstant

    fun advance(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }
}