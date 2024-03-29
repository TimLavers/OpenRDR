package io.rippledown.persistence.postgres

import io.rippledown.kb.AttributeProvider
import io.rippledown.model.*
import io.rippledown.persistence.CaseStore
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class PostgresCaseStore(private val db: Database): CaseStore {
    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGCaseIds)
            SchemaUtils.create(PGCaseValues)
        }
    }

    override fun allCaseIds() = transaction(db) {
        return@transaction PGCaseId.all()
            .map { CaseId(it.id.value, it.name!!, CaseType.values()[it.type!!]) }
            .sortedBy { it.id!! }
    }

    override fun all(attributeProvider: AttributeProvider): List<RDRCase> {
        val allCaseIds = allCaseIds()
        return transaction(db) {
            return@transaction allCaseIds.map { RDRCase(it, getCaseData(it.id!!, attributeProvider)) }
        }
    }

    override fun put(case: RDRCase): RDRCase {
        require (case.id == null) {
            "Case has an id already, please use update instead."
        }
        var id: Long? = null
        transaction(db) {
            val pgCaseId = storeCaseId(case)
            id = pgCaseId.id.value
            storeCaseData(case, id)
        }
        // The only change to the case is that its id is set.
        val caseId = CaseId(id!!, case.name, case.caseId.type)
        return RDRCase(caseId, case.data)
    }

    override fun load(cases: List<RDRCase>) {
        require(allCaseIds().isEmpty()) {
            "Cannot load cases if there are already some present."
        }
        require(cases.find { it.id == null } == null) {
            "Cannot load cases unless they already have their ids set."
        }
        transaction(db) {
            cases.forEach {
                storeCaseId(it)
                storeCaseData(it, it.id!!)
            }
        }
    }

    override fun get(id: Long, attributeProvider: AttributeProvider): RDRCase? =  transaction(db) {
            val pgCaseId = PGCaseId.findById(id) ?: return@transaction null
            val data = getCaseData(id, attributeProvider)
            return@transaction RDRCase(caseId(pgCaseId), data)
        }

    private fun getCaseData(
        id: Long,
        attributeProvider: AttributeProvider
    ) = PGCaseValue.find {
        PGCaseValues.caseId eq id
    }.associateBy({ TestEvent(attributeProvider.getById(it.attributeId), it.date) }, { testResult(it) })

    override fun delete(id: Long) {
        transaction(db) {
            val pgCaseId = PGCaseId.findById(id) ?: return@transaction
            PGCaseValues.deleteWhere {
                caseId eq pgCaseId.id.value
            }
            pgCaseId.delete()
        }
    }

    /**
     * The number of stored case data items (TestEvent, TestResult) pairs.
     * Mainly provided for testing.
     */
    fun dataPointsCount() = transaction(db) {
        return@transaction PGCaseValue.count()
    }

    private fun storeCaseId(case: RDRCase) = PGCaseId.new(case.id) {
        name = case.caseId.name
        type = case.caseId.type.ordinal
    }

    private fun storeCaseData(case: RDRCase, id: Long?) {
        case.data.forEach {
            PGCaseValue.new {
                caseId = id!!
                attributeId = it.key.attribute.id
                date = it.key.date
                value = it.value.value.text
                units = it.value.units
                rangeLow = it.value.referenceRange?.lowerString
                rangeHigh = it.value.referenceRange?.upperString
            }
        }
    }

    private fun caseId(pgCaseId: PGCaseId): CaseId {
        val type = CaseType.values()[pgCaseId.type!!]
        return CaseId(pgCaseId.id.value, pgCaseId.name!!, type)
    }

    private fun testResult(pgCaseValue: PGCaseValue): TestResult {
        val range = if (pgCaseValue.rangeLow == null && pgCaseValue.rangeHigh == null) null else ReferenceRange(pgCaseValue.rangeLow, pgCaseValue.rangeHigh)
        return TestResult(pgCaseValue.value, range, pgCaseValue.units)
    }
}
object PGCaseIds: LongIdTable(name = "case_ids") {
    val name = varchar("name", 256).nullable() // todo limit this in CaseId
    val type = integer("type").nullable()
}
class PGCaseId(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<PGCaseId>(PGCaseIds)
    var name by PGCaseIds.name
    var type by PGCaseIds.type
}
object PGCaseValues: LongIdTable(name = "case_values") {
    val caseId = long("case")
    val attributeId = integer("attribute")
    val date = long("date")
    val value = text("value")
    val units = varchar("units", 256).nullable() // todo limit this
    val rangeLow = varchar("range_low", 256).nullable() // todo limit this
    val rangeHigh = varchar("range_high", 256).nullable() // todo limit this
}
class PGCaseValue(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<PGCaseValue>(PGCaseValues)
    var caseId by PGCaseValues.caseId
    var attributeId by PGCaseValues.attributeId
    var date by PGCaseValues.date
    var value by PGCaseValues.value
    var units by PGCaseValues.units
    var rangeLow by PGCaseValues.rangeLow
    var rangeHigh by PGCaseValues.rangeHigh
}