package club.kosya.lib.executionengine.internal

import org.springframework.data.jpa.repository.JpaRepository

interface ExecutionsRepository : JpaRepository<Execution, Long>
