package dbdata.migration

import dbdata.migration.SchemaVersionTable.appliedAt
import dbdata.migration.SchemaVersionTable.scriptName
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.security.MessageDigest
import java.time.Instant

// Data class to hold migration script details
data class MigrationScript(
    val version: Int,
    val name: String,
    val content: String,
    val checksum: String
)

// Exposed table to track schema version
object SchemaVersionTable : Table("dabi_schema_version") {
    val version = integer("version").uniqueIndex()
    val scriptName = varchar("script_name", 255)
    val checksum = varchar("checksum", 64) // SHA-256
    val appliedAt = timestamp("applied_at")
    override val primaryKey = PrimaryKey(version)
}

class DatabaseMigrator(
    private val database: Database,
    private val migrationFolderPath: String = "src/main/resources/db/migration"
) {

    fun migrate() {
        transaction(database) {
            // 1. Ensure schema history table exists
            SchemaUtils.createMissingTablesAndColumns(SchemaVersionTable)
        }

        // 2. Find local migration scripts
        val localScripts = findLocalMigrationScripts()

        // 3. Get applied migrations from DB
        val appliedScripts = getAppliedMigrations()

        // 4. Validate applied migrations (check for checksum changes)
        validateAppliedMigrations(localScripts, appliedScripts)

        // 5. Determine and run new migrations
        val newScripts = localScripts.filter { it.version > (appliedScripts.keys.maxOrNull() ?: 0) }
            .sortedBy { it.version }

        if (newScripts.isNotEmpty()) {
            println("Found ${newScripts.size} new migrations to apply.")
            newScripts.forEach { script ->
                println("Applying migration V${script.version}__${script.name}...")
                transaction(database) {
                    // Execute the script content
                    script.content.split(';').forEach { statement ->
                        if (statement.trim().isNotBlank()) {
                            exec(statement)
                        }
                    }

                    // Record the migration in the history table
                    SchemaVersionTable.insert {
                        it[version] = script.version
                        it[scriptName] = script.name
                        it[checksum] = script.checksum
                        it[appliedAt] = Instant.now()
                    }
                }
                println("Successfully applied migration V${script.version}__${script.name}.")
            }
        } else {
            println("Database schema is up to date.")
        }
    }

    private fun findLocalMigrationScripts(): List<MigrationScript> {
        val migrationDir = File(migrationFolderPath)
        if (!migrationDir.exists() || !migrationDir.isDirectory) {
            println("Migration directory not found at: ${migrationDir.absolutePath}")
            return emptyList()
        }

        val scriptPattern = """V(\d+)__(.+)\.sql""".toRegex()

        return migrationDir.listFiles()
            ?.filter { it.isFile && it.name.matches(scriptPattern) }
            ?.mapNotNull { file ->
                val match = scriptPattern.find(file.name)
                if (match != null) {
                    val version = match.groupValues[1].toInt()
                    val name = match.groupValues[2].replace("_", " ")
                    val content = file.readText()
                    val checksum = content.toSha256()
                    MigrationScript(version, name, content, checksum)
                } else {
                    null
                }
            } ?: emptyList()
    }

    private fun getAppliedMigrations(): Map<Int, Pair<String, String>> {
        return transaction(database) {
            SchemaVersionTable.selectAll()
                .orderBy(SchemaVersionTable.version, SortOrder.ASC)
                .associate {
                    it[SchemaVersionTable.version] to (it[SchemaVersionTable.scriptName] to it[SchemaVersionTable.checksum])
                }
        }
    }

    private fun validateAppliedMigrations(
        localScripts: List<MigrationScript>,
        appliedScripts: Map<Int, Pair<String, String>>
    ) {
        localScripts.forEach { localScript ->
            appliedScripts[localScript.version]?.let { (_, appliedChecksum) ->
                if (localScript.checksum != appliedChecksum) {
                    throw IllegalStateException(
                        "Checksum mismatch for applied migration V${localScript.version}__${localScript.name}! " +
                        "The script has been modified after it was applied."
                    )
                }
            }
        }
    }

    private fun String.toSha256(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(this.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}
