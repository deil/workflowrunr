package club.kosya.lib.executionengine

import org.springframework.data.jpa.repository.JpaRepository

interface ExecutionsRepository : JpaRepository<Execution, Long>
