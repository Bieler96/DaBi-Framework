package dbdata

import java.time.Instant

interface Entity<ID> : Auditable<ID> {
	val id: ID?
    override var createdAt: Instant?
    override var updatedAt: Instant?
    override var createdBy: ID?
    override var updatedBy: ID?
}