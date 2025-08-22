package dbdata

import java.time.Instant
import java.time.LocalDateTime

interface Auditable {
    var createdAt: Instant?
    var updatedAt: Instant?
    var createdBy: String?
    var updatedBy: String?
}
