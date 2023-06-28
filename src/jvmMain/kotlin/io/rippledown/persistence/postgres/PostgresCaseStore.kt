package io.rippledown.persistence.postgres

import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.CaseType
import io.rippledown.model.RDRCase
import io.rippledown.persistence.CaseStore
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

class PostgresCaseStore(private val db: Database): CaseStore {
    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGCaseIds)
        }
    }

    override fun allCaseIds()= transaction(db) {
        return@transaction PGCaseId.all().map { CaseId(it.id.value, it.name, CaseType.values()[it.type]) }
    }

    override fun all(): List<RDRCase> {
        TODO("Not yet implemented")
    }

    override fun create(case: RDRCase): RDRCase {
        require (case.id == null) {
            "Case has an id already, please use update instead."
        }
        var pgCaseId: PGCaseId? = null
        transaction(db) {
            pgCaseId = PGCaseId.new {
                name = case.caseId.name
                type = case.caseId.type.ordinal
            }
        }
        val caseId = CaseId(pgCaseId!!.id.value, pgCaseId!!.name, case.caseId.type)
//        return Conclusion(pgConclusion!!.id.value, pgConclusion!!.conclusionText)

        return RDRCase(caseId, case.data)
    }

    override fun update(case: RDRCase) {
        TODO("Not yet implemented")
    }

    override fun load(cases: List<RDRCase>) {
        TODO("Not yet implemented")
    }

    override fun get(id: Long): RDRCase? {
        TODO("Not yet implemented")
    }

    override fun delete(id: Long): Boolean {
        TODO("Not yet implemented")
    }
}
object PGCaseIds: LongIdTable(name = "case_ids") {
    val name = varchar("name", 256) // todo limit this in CaseId
    val type = integer("type")
}
class PGCaseId(id: EntityID<Long>): LongEntity(id){
    companion object: LongEntityClass<PGCaseId>(PGCaseIds)
    var name by PGCaseIds.name
    var type by PGCaseIds.type
}