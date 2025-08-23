package dbdata

import java.time.Instant

interface Auditable {
    var createdAt: Instant?
    var updatedAt: Instant?
    var createdBy: String?
    var updatedBy: String?
}