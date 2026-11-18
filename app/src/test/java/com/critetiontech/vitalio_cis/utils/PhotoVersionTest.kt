package com.critetiontech.ctvitalio.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoVersionTest {

    private val url = "https://cis.vitalio.care:4082/Upload/Image/patient_1107.jpg"

    @Test fun `adds a version to a plain url`() =
        assertEquals("$url?v=5", withPhotoVersion(url, 5))

    @Test fun `replaces an earlier version instead of stacking`() =
        assertEquals("$url?v=9", withPhotoVersion("$url?v=5", 9))

    @Test fun `keeps other query parameters`() =
        assertEquals("$url?size=200&v=9", withPhotoVersion("$url?v=5&size=200", 9))

    @Test fun `blank url stays blank`() =
        assertEquals("", withPhotoVersion("", 9))
}
