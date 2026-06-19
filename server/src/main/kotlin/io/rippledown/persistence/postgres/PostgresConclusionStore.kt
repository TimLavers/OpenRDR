package io.rippledown.persistence.postgres

import io.rippledown.model.CommentVariable
import io.rippledown.model.Conclusion
import io.rippledown.persistence.ConclusionStore
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

const val CONCLUSIONS_TABLE = "conclusions"
const val CONCLUSION_VARIABLES_TABLE = "conclusion_variables"

class PostgresConclusionStore(private val db: Database): ConclusionStore {

    init {
        transaction(db) {
            // addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGConclusions, PGConclusionVariables)
        }
    }

    override fun all() = transaction(db) {
        return@transaction PGConclusion.all().map { pgConclusion ->
            val variables = pgConclusion.variables.map { pgVar ->
                CommentVariable(pgVar.charIndex, pgVar.attributeId)
            }
            Conclusion(pgConclusion.id.value, pgConclusion.conclusionText, variables)
        }.toSet()
    }

    override fun create(text: String): Conclusion {
        return create(text, emptyList())
    }

    override fun create(text: String, variables: List<CommentVariable>): Conclusion {
        val isNew = all().count { it.text == text && it.variables == variables } == 0
        require(isNew) {
            "A conclusion with the given text already exists."
        }
        var pgConclusion: PGConclusion? = null
        transaction(db) {
            pgConclusion = PGConclusion.new {
                conclusionText = text
            }
            variables.forEach { variable ->
                PGConclusionVariable.new {
                    this.conclusion = pgConclusion!!
                    charIndex = variable.charIndex
                    attributeId = variable.attributeId
                }
            }
        }
        return Conclusion(pgConclusion!!.id.value, pgConclusion.conclusionText, variables)
    }

    override fun store(conclusion: Conclusion) =
        transaction(db) {
            val pgConclusion = PGConclusion[conclusion.id]
            pgConclusion.conclusionText = conclusion.text

            // Delete existing variables and reinsert
            pgConclusion.variables.forEach { it.delete() }
            conclusion.variables.forEach { variable ->
                PGConclusionVariable.new {
                    this.conclusion = pgConclusion
                    charIndex = variable.charIndex
                    attributeId = variable.attributeId
                }
            }
        }

    override fun load(conclusions: Set<Conclusion>) {
        require(all().isEmpty()) {
            "Cannot load conclusions if there are are some stored already."
        }
        transaction(db) {
            conclusions.forEach { conclusion ->
                val pgConclusion = PGConclusion.new(conclusion.id) {
                    conclusionText = conclusion.text
                }
                conclusion.variables.forEach { variable ->
                    PGConclusionVariable.new {
                        this.conclusion = pgConclusion
                        charIndex = variable.charIndex
                        attributeId = variable.attributeId
                    }
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
    val variables by PGConclusionVariable referrersOn PGConclusionVariables.conclusion
}

object PGConclusionVariables : IntIdTable(name = CONCLUSION_VARIABLES_TABLE) {
    val conclusion = reference("conclusion_id", PGConclusions)
    val charIndex = integer("char_index")
    val attributeId = integer("attribute_id")
}

class PGConclusionVariable(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PGConclusionVariable>(PGConclusionVariables)

    var conclusion by PGConclusion referencedOn PGConclusionVariables.conclusion
    var charIndex by PGConclusionVariables.charIndex
    var attributeId by PGConclusionVariables.attributeId
}