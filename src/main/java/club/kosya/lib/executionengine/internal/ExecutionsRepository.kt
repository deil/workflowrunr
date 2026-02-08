package club.kosya.lib.executionengine.internal

import club.kosya.lib.executionengine.ExecutionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ExecutionsRepository : JpaRepository<Execution, Long> {
    fun findByStatus(status: ExecutionStatus): List<Execution>

    @Query("SELECT e FROM Execution e WHERE e.wakeAt <= :now AND e.status = 'Running'")
    fun findRunnableByWakeAtLessThanEqual(@Param("now") now: Instant): List<Execution>
}
