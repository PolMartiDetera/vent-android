package com.vent.app.data.repo

import com.vent.app.data.db.LocationDao
import com.vent.app.data.db.LocationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LocationRepository(
    private val dao: LocationDao,
) {
    fun observeAll(): Flow<List<LocationEntity>> = dao.observeAll()

    suspend fun get(id: Long): LocationEntity? = dao.get(id)

    suspend fun default(): LocationEntity? = dao.getDefault()

    suspend fun add(name: String, lat: Double, lon: Double, makeDefault: Boolean): Long {
        val count = dao.observeAll().first().size
        return dao.insert(
            LocationEntity(
                name = name,
                lat = lat,
                lon = lon,
                isDefault = makeDefault || count == 0,
                sortOrder = count,
            )
        )
    }

    suspend fun rename(id: Long, name: String) {
        dao.get(id)?.let { dao.update(it.copy(name = name)) }
    }

    suspend fun setDefault(id: Long) {
        val all = dao.observeAll().first()
        all.forEach { dao.update(it.copy(isDefault = it.id == id)) }
    }

    suspend fun reorder(id: Long, newOrder: Int) {
        dao.get(id)?.let { dao.update(it.copy(sortOrder = newOrder)) }
    }

    suspend fun remove(id: Long) {
        dao.get(id)?.let { dao.delete(it) }
    }
}