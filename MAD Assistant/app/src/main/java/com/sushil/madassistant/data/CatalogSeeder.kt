package com.sushil.madassistant.data

import com.sushil.madassistant.data.db.CategoryEntity
import com.sushil.madassistant.data.db.ItemEntity
import com.sushil.madassistant.data.db.MadDatabase

object CatalogSeeder {

    private suspend fun addItems(
        dao: com.sushil.madassistant.data.db.CatalogDao,
        categoryId: Long,
        items: List<Pair<String, String>>
    ) {
        items.forEach { (n, u) -> dao.insertItem(ItemEntity(categoryId = categoryId, name = n, unit = u)) }
    }

    suspend fun seedIfNeeded(db: MadDatabase) {
        val dao = db.catalogDao()
        if (dao.getCategoryCount() > 0) return

        fun cat(workType: String, name: String, parent: Long = 0) =
            CategoryEntity(workType = workType, name = name, parentId = parent)

        val openId = dao.insertCategory(cat("Electrical", "Open Fitting"))
        addItems(dao, openId, listOf(
            "Board 4x4" to "Piece", "Board 7x4" to "Piece", "Board 8x5" to "Piece",
            "Sheet 4x4" to "Piece", "Sheet 7x4" to "Piece", "Sheet 8x5" to "Piece"
        ))

        val ugId = dao.insertCategory(cat("Electrical", "Underground Fitting"))
        val lanternId = dao.insertCategory(cat("Electrical", "Lantern Fitting", ugId))
        val wallCutId = dao.insertCategory(cat("Electrical", "Wall Cutting", ugId))
        val finalId = dao.insertCategory(cat("Electrical", "Final Fitting", ugId))

        addItems(dao, lanternId, listOf(
            "PVC Conduit Pipe" to "Bundle", "Conduit Accessories" to "Set",
            "Fan Box" to "Piece", "Junction Box" to "Piece", "Bend" to "Piece", "Coupler" to "Piece"
        ))

        addItems(dao, wallCutId, listOf(
            "Conduit Pipe" to "Bundle", "Modular Box" to "Piece", "Wall Cutting Blade" to "Piece"
        ))

        addItems(dao, finalId, listOf(
            "Wire 1.5mm" to "Roll", "Wire 2.5mm" to "Roll", "Wire 4mm" to "Roll",
            "6A Switch" to "Piece", "16A Switch" to "Piece", "Socket 6A" to "Piece",
            "Socket 16A" to "Piece", "Modular Plate" to "Piece", "Designer Plate" to "Piece",
            "Holder" to "Piece", "LED Bulb" to "Piece"
        ))

        val plumbId = dao.insertCategory(cat("Plumbing", "Pipes and Fittings"))
        addItems(dao, plumbId, listOf(
            "CPVC Pipe 1/2 in" to "Meter", "CPVC Pipe 3/4 in" to "Meter",
            "Elbow 90" to "Piece", "Tee" to "Piece", "Ball Valve" to "Piece"
        ))

        val weldId = dao.insertCategory(cat("Welding", "Welding Materials"))
        addItems(dao, weldId, listOf(
            "Welding Rod 10G" to "Packet", "Welding Rod 12G" to "Packet",
            "Grinding Wheel 4 in" to "Piece", "Cutting Wheel 4 in" to "Piece", "Flap Disc" to "Piece"
        ))

        val mechId = dao.insertCategory(cat("Mechanical", "Fasteners and Bolts"))
        addItems(dao, mechId, listOf(
            "Hex Bolt M8" to "Piece", "Hex Bolt M10" to "Piece",
            "Washer Flat" to "Packet", "Bearing 6203" to "Piece"
        ))
    }
}
