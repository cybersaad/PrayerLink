package com.prayerlink.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {

    private val repo = UpdateRepository()

    @Test
    fun testParseVersion() {
        val parsed1 = repo.parseVersion("1.0.0")
        assertTrue(parsed1 == listOf(1, 0, 0))

        val parsed2 = repo.parseVersion("v1.2.0")
        assertTrue(parsed2 == listOf(1, 2, 0))

        val parsed3 = repo.parseVersion("v2")
        assertTrue(parsed3 == listOf(2))

        val parsed4 = repo.parseVersion("1.2.3-rc1") // Non-numeric are stripped by regex
        assertTrue(parsed4 == listOf(1, 2, 3, 1)) // 1.2.3.1 (rc1 stripped to 1) -> This is why we use semantic comparison
    }

    @Test
    fun testVersionComparison() {
        // Newer versions
        assertTrue(repo.isVersionGreater(listOf(1, 0, 1), listOf(1, 0, 0)))
        assertTrue(repo.isVersionGreater(listOf(1, 1, 0), listOf(1, 0, 0)))
        assertTrue(repo.isVersionGreater(listOf(2, 0, 0), listOf(1, 9, 9)))
        assertTrue(repo.isVersionGreater(listOf(1, 2, 0), listOf(1, 1)))

        // Older versions
        assertFalse(repo.isVersionGreater(listOf(1, 0, 0), listOf(1, 0, 1)))
        assertFalse(repo.isVersionGreater(listOf(1, 1), listOf(1, 2, 0)))

        // Equal versions
        assertFalse(repo.isVersionGreater(listOf(1, 0, 0), listOf(1, 0, 0)))
        assertFalse(repo.isVersionGreater(listOf(1, 1), listOf(1, 1, 0)))
        
        // Edge cases
        assertFalse(repo.isVersionGreater(listOf(), listOf(1)))
        assertTrue(repo.isVersionGreater(listOf(1), listOf()))
    }
}
