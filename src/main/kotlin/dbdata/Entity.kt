package dbdata

import java.time.LocalDateTime

interface Entity<ID> : Auditable {
	val id: ID?
    override var createdAt: LocalDateTime?
    override var updatedAt: LocalDateTime?
    override var createdBy: String?
    override var updatedBy: String?
}