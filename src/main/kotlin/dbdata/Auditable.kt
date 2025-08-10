package dbdata

import java.time.LocalDateTime

interface Auditable {
    var createdAt: LocalDateTime?
    var updatedAt: LocalDateTime?
    var createdBy: String?
    var updatedBy: String?
}
