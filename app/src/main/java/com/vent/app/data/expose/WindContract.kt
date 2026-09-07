package com.vent.app.data.expose

import android.net.Uri

/** Contract definitions for the wind ContentProvider schema.
  * Exposes wind data at content://com.vent.app.wind/wind
  * Columns: speed, unit, direction, gust, timestamp
  */
object WindContract {
    const val AUTHORITY = "com.vent.app.wind"
    const val PATH = "wind"

    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH")

    /** Column names */
    object Columns {
        const val SPEED = "speed"
        const val UNIT = "unit"
        const val DIRECTION = "direction"
        const val GUST = "gust"
        const val TIMESTAMP = "timestamp"
    }
}