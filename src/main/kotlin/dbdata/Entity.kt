package dbdata

import java.time.Instant

interface Entity<ID> : Auditable {
	val id: ID?
    override var createdAt: Instant?
    override var updatedAt: Instant?
    override var createdBy: String?
    override var updatedBy: String?
}