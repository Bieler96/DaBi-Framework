package dbdata

import java.time.Instant

interface Auditable<ID> {
    var createdAt: Instant?
    var updatedAt: Instant?
    var createdBy: ID?
    var updatedBy: ID?
}