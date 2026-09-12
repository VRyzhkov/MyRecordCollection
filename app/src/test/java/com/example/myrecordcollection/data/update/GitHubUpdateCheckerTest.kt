package com.example.myrecordcollection.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateCheckerTest {
    private val checker = GitHubUpdateChecker()

    @Test
    fun newerSemanticVersionIsDetected() {
        assertTrue(checker.isNewer(candidate = "0.1.0", current = "0.0.9"))
        assertTrue(checker.isNewer(candidate = "1.0.0", current = "0.99.99"))
    }

    @Test
    fun sameOlderAndInvalidVersionsAreIgnored() {
        assertFalse(checker.isNewer(candidate = "1.2.3", current = "1.2.3"))
        assertFalse(checker.isNewer(candidate = "1.2.2", current = "1.2.3"))
        assertFalse(checker.isNewer(candidate = "latest", current = "1.2.3"))
    }
}
