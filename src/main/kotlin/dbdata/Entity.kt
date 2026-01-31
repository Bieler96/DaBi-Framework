package dbdata

import java.time.Instant

interface Entity<ID> {
	val id: ID?
	var createdAt: Instant?
	var updatedAt: Instant?
}