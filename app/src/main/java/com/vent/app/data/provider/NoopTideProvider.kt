package com.vent.app.data.provider

import com.vent.app.data.model.TideData

/**
 * Default tide source: no tide data (tideSource = "none"). A WorldTides-backed
 * implementation can be supplied later without touching callers.
 */
class NoopTideProvider : TideProvider {
    override suspend fun tide(lat: Double, lon: Double): TideData = TideData(
        highTideAt = null,
        lowTideAt = null,
        heightMeters = 0.0,
    )
}