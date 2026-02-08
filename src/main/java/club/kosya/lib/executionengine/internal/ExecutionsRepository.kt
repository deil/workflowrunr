package club.kosya.lib.executionengine.internal

import club.kosya.lib.executionengine.ExecutionStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface ExecutionsRepository : JpaRepository<Execution, Long> {
    fun findByStatus(status: ExecutionStatus): List<Execution>

    fun findByWakeAtLessThanEqual(now: Instant): List<Execution>
}
