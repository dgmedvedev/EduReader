package com.example.edureader.presentation.common

import com.example.edureader.R
import com.example.edureader.domain.common.DomainError
import com.example.edureader.presentation.reader.contract.TextSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainErrorUiMapperTest {

    @Test
    fun `maps validation error to validation text`() {
        assertResId(DomainError.Validation("v"), R.string.reader_error_validation)
    }

    @Test
    fun `maps not found error to not found text`() {
        assertResId(DomainError.NotFound("n"), R.string.reader_error_not_found)
    }

    @Test
    fun `maps storage error to storage text`() {
        assertResId(DomainError.Storage("s"), R.string.reader_error_storage)
    }

    @Test
    fun `maps parsing error to parsing text`() {
        assertResId(DomainError.Parsing("p"), R.string.reader_error_parsing)
    }

    @Test
    fun `maps unknown error to generic text`() {
        assertResId(DomainError.Unknown("u"), R.string.reader_error_generic)
    }

    private fun assertResId(error: DomainError, expectedResId: Int) {
        val textSpec = error.toTextSpec()
        assertTrue(textSpec is TextSpec.Res)
        assertEquals(expectedResId, (textSpec as TextSpec.Res).id)
    }
}
