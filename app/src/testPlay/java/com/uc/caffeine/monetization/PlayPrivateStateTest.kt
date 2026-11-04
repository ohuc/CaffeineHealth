package com.uc.caffeine.monetization

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PlayPrivateStateTest {
    @get:Rule val directory = TemporaryFolder()
    private val day = 86_400_000L

    @Test fun `no free day exists until the user starts it`() {
        val file = File(directory.root, "private.properties")
        assertNull(PlayPrivateState(file).trial(1_000L))
        assertFalse(file.exists())
    }
    @Test fun `a started free day survives process recreation and lasts one day`() {
        val file = File(directory.root, "private.properties")
        PlayPrivateState(file).startTrial(1_000L)
        val later = PlayPrivateState(file).trial(90_000L)
        assertEquals(1_000L, later?.startedAtMillis)
        assertEquals(1_000L + day, later?.endsAtMillis)
    }
    @Test fun `starting again never restarts the free day`() {
        val file = File(directory.root, "private.properties")
        PlayPrivateState(file).startTrial(1_000L)
        assertEquals(1_000L, PlayPrivateState(file).startTrial(5 * day).startedAtMillis)
    }
    @Test fun `a corrupt record counts the free day as used`() {
        val file = File(directory.root, "private.properties")
        file.writeText("trial.started=broken\n")
        val state = PlayPrivateState(file).trial(1_000_000_000L)
        assertNotNull(state)
        assertFalse(AccessPolicy.resolve(null, state, 1_000_000_000L).hasPlus)
    }
    @Test fun `a clock moved back cannot lengthen the free day`() {
        val file = File(directory.root, "private.properties")
        PlayPrivateState(file).startTrial(10 * day)
        PlayPrivateState(file).trial(10 * day + day / 2)
        val rolledBack = PlayPrivateState(file).trial(5 * day)
        assertFalse(AccessPolicy.resolve(null, rolledBack, 5 * day).hasPlus)
    }
    @Test fun `journey progress survives process recreation`() {
        val file = File(directory.root, "private.properties")
        val journey = JourneyRecord(openDays = 3, lastOpenEpochDay = 42L, showcasePending = true, offerShown = true)
        PlayPrivateState(file).saveJourney(journey)
        assertEquals(journey, PlayPrivateState(file).journey)
    }
    @Test fun `private grant round trip keeps both lifetime and subscription`() {
        val file = File(directory.root, "private.properties")
        val grant = PaidGrant(true, true, 100L, null, true, true)
        PlayPrivateState(file).saveGrant(grant)
        assertEquals(grant, PlayPrivateState(file).grant)
    }
    @Test fun `status dismissal is private and survives restart`() {
        val file = File(directory.root, "private.properties")
        PlayPrivateState(file).dismissStatus("expired")
        assertTrue(PlayPrivateState(file).isStatusDismissed("expired"))
        assertFalse(PlayPrivateState(file).isStatusDismissed("billing"))
    }
    @Test fun `pending checkout survives a process restart without granting access`() {
        val file = File(directory.root, "private.properties")
        PlayPrivateState(file).setPending(true)
        val restored = PlayPrivateState(file)
        assertTrue(restored.pending)
        assertNull(restored.grant)
        restored.setPending(false)
        assertFalse(PlayPrivateState(file).pending)
    }
}
