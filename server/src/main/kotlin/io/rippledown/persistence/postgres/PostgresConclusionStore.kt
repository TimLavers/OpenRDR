package io.rippledown.persistence.postgres

import io.rippledown.model.Conclusion
import io.rippledown.persistence.ConclusionStore
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

const val CONCLUSIONS_TABLE = "conclusions"

class PostgresConclusionStore(private val db: Database): ConclusionStore {

    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGConclusions)
        }
    }

    override fun all() = transaction(db) {
            return@transaction PGConclusion.all().map {Conclusion(it.id.value, it.conclusionText)}.toSet()
    }

    override fun create(text: String): Conclusion {
        val isNew = all().count { it.text == text } == 0
        require(isNew) {
            "A conclusion with the given text already exists."
        }
        var pgConclusion: PGConclusion? = null
        transaction(db) {
            pgConclusion = PGConclusion.new {
                conclusionText = text
            }
        }
        return Conclusion(pgConclusion!!.id.value, pgConclusion!!.conclusionText)
    }

    override fun store(conclusion: Conclusion) =
        transaction(db) {
            PGConclusion[conclusion.id].conclusionText = conclusion.text
        }

    override fun load(conclusions: Set<Conclusion>) {
        require(all().isEmpty()) {
            "Cannot load conclusions if there are are some stored already."
        }
        transaction(db) {
            conclusions.forEach{
                PGConclusion.new(it.id) {
                    conclusionText = it.text
                }
            }
        }
    }
}
object PGConclusions: IntIdTable(name = CONCLUSIONS_TABLE) {
    val conclusionText = varchar("text", 2048)
}
class PGConclusion(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<PGConclusion>(PGConclusions)
    var conclusionText by PGConclusions.conclusionText
}