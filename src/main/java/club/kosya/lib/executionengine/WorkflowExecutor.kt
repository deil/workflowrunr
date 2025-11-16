package club.kosya.lib.executionengine

import club.kosya.lib.workflow.ExecutionContext
import club.kosya.lib.workflow.WorkflowDefinition
import club.kosya.lib.workflow.internal.WorkflowReconstructor
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class WorkflowExecutor(
    private val objectMapper: ObjectMapper,
    private val workflowReconstructor: WorkflowReconstructor,
    private val executions: ExecutionsRepository,
) {
    // @Scheduled(fixedDelay = 1000L)
    fun tick() {
        executions
            .findAll()
            .forEach { wf ->
                execute(wf)
            }
    }

    private fun execute(execution: Execution) {
        log.info("Executing workflow: executionId={}", execution.id)

        if (execution.status != ExecutionStatus.Queued) {
            log.warn(
                "Execution {} is not in Queued status, skipping. Current status: {}",
                execution.id,
                execution.status,
            )
            return
        }

        execution.status = ExecutionStatus.Running
        execution.startedAt = LocalDateTime.now()
        executions.save(execution)

        try {
            val definitionJson = String(execution.definition)
            val definition = objectMapper.readValue(definitionJson, WorkflowDefinition::class.java)

            log.info(
                "Workflow definition: beanClass={}, method={}, params={}",
                definition.serviceIdentifier.className,
                definition.methodName,
                definition.parameters,
            )

            val executionContext =
                club.kosya.lib.executionengine.internal.ExecutionContextImplementation(
                    execution.id.toString(),
                    objectMapper,
                    executions,
                )

            val result = workflowReconstructor.reconstructAndExecute(definition, executionContext)

            execution.status = ExecutionStatus.Completed
            execution.completedAt = LocalDateTime.now()
            executions.save(execution)

            log.info(
                "Workflow completed successfully: executionId={}, result={}",
                execution.id,
                result,
            )
        } catch (e: Exception) {
            log.error("Workflow execution failed: executionId=${execution.id}", e)

            execution.status = ExecutionStatus.Failed
            execution.completedAt = LocalDateTime.now()
            executions.save(execution)

            throw RuntimeException("Workflow execution failed", e)
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(WorkflowExecutor::class.java)
    }
}
