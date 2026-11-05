package app.pantauizin.data

import android.content.Context
import app.pantauizin.domain.SantriCsvParser

class SantriSeeder(
    private val dao: SantriDao,
    private val assetReader: () -> String
) {
    constructor(context: Context, dao: SantriDao) : this(
        dao = dao,
        assetReader = {
            context.assets.open("santri_master.csv")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }
    )

    suspend fun seedIfEmpty(): Int {
        if (dao.count() > 0) return 0
        val rows = SantriCsvParser.parse(assetReader())
        val entities = rows.map { row ->
            SantriEntity(nama = row.nama, kamar = row.kamar, aktif = true)
        }
        dao.insertAll(entities)
        return entities.size
    }
}

