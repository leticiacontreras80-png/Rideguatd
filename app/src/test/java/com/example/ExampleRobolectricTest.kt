package com.example

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.example.service.RideLocationServiceHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RideGuard", appName)
  }

  @Test
  fun `verify rideguard strings exist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val panicText = context.getString(R.string.panic_button)
    assertNotNull(panicText)
    assertEquals("PANIC SOS", panicText)
  }

  @Test
  fun `verify ride location service helper updates coordinates`() {
    val mockLocation = Location("gps").apply {
      latitude = 37.7749
      longitude = -122.4194
      speed = 15.0f // m/s
    }

    RideLocationServiceHelper.updateLocation(mockLocation, "Market St, San Francisco")

    val location = RideLocationServiceHelper.currentLocation.value
    assertNotNull(location)
    assertEquals(37.7749, location!!.latitude, 0.0001)
    assertEquals(-122.4194, location.longitude, 0.0001)
    assertEquals("Market St, San Francisco", RideLocationServiceHelper.currentAddress.value)
    // 15 m/s * 2.23694 = ~33.55 mph
    assertEquals(15f * 2.23694f, RideLocationServiceHelper.currentSpeedMph.value, 0.1f)
  }
}
