package com.vent.app.data.expose

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/** ContentProvider that serves wind data via the WindContract URI.
  * Read-only provider; query returns records with columns: speed, unit, direction, gust, timestamp
  */
class WindContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        // TODO: Implement cursor return over wind records matching WindContract
        return null
    }

    override fun getType(uri: Uri): String? {
        // TODO: Implement type detection
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        // TODO: Implement insert
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        // TODO: Implement delete
        return 0
    }

    override fun update(
        uri: Uri,
        contentValues: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int {
        // TODO: Implement update
        return 0
    }
}