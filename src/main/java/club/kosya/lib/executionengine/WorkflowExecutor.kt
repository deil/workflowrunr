package club.kosya.lib.executionengine

import club.kosya.lib.deserialization.internal.ObjectDeserializerImpl
import club.kosya.lib.executionengine.internal.ExecutionContextImpl
import club.kosya.lib.executionengine.internal.ExecutionsRepository
import club.kosya.lib.executionengine.internal.WorkflowCanceledException
import club.kosya.lib.executionengine.internal.WorkflowSuspendedException
import club.kosya.lib.workflow.ServiceInstanceProvider
import club.kosya.lib.workflow.WorkflowDefinition
import club.kosya.lib.workflow.internal.WorkflowReconstructor
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.reflect.InvocationTargetException
import java.time.Instant
import java.time.LocalDateTime

@Component
class WorkflowExecutor(
    private val objectMapper: ObjectMapper,
    private val executions: ExecutionsRepository,
    instanceProvider: ServiceInstanceProvider,
) {
    private val objectDeserializer = ObjectDeserializerImpl(objectMapper)
    private val workflowReconstructor = WorkflowReconstructor(instanceProvider, objectDeserializer)

    @Scheduled(fixedDelay = 1000L)
    fun tick() {
        // Poll queued workflows
        executions.findByStatus(ExecutionStatus.Queued).forEach { wf ->
            execute(wf.id, isResume = false)
        }

        // Poll sleeping workflows (Running with past wakeAt, excluding Cancelled)
        executions.findRunnableByWakeAtLessThanEqual(Instant.now()).forEach { wf ->
            log.info("Resuming sleeping workflow: executionId={}", wf.id)
            execute(wf.id, isResume = true)
        }
    }

    private fun execute(
        executionId: Long,
        isResume: Boolean = false,
    ) {
        var execution = executions.findById(executionId).get()
        log.info("Executing workflow: executionId={}, isResume={}", execution.id, isResume)

        if (execution.status == ExecutionStatus.Queued) {
            execution.status = ExecutionStatus.Running
            execution.startedAt = LocalDateTime.now()
        }

        // Clear wakeAt if resuming from sleep
        if (isResume) {
            execution.wakeAt = null
        }

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
                ExecutionContextImpl(
                    execution.id.toString(),
                    objectMapper,
                    executions,
                    objectDeserializer,
                )

            val result = workflowReconstructor.reconstructAndExecute(definition) { executionContext }

            execution = executions.findById(execution.id).get()
            execution.status = ExecutionStatus.Completed
            execution.completedAt = LocalDateTime.now()
            executions.save(execution)

            log.info(
                "Workflow completed successfully: executionId={}, result={}",
                execution.id,
                result,
            )
        } catch (e: WorkflowSuspendedException) {
            log.info("Workflow suspended (sleeping): executionId=${execution.id}, reason=${e.message}")
            return
        } catch (e: WorkflowCanceledException) {
            log.info("Workflow was cancelled during execution: executionId=${execution.id}")
            return
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            if (cause is WorkflowSuspendedException) {
                log.info("Workflow suspended (sleeping): executionId=${execution.id}, reason=${cause.message}")
                return
            }
            if (cause is WorkflowCanceledException) {
                log.info("Workflow was cancelled during execution: executionId=${execution.id}")
                return
            }
            log.error("Workflow execution failed: executionId=${execution.id}", e)
            execution.status = ExecutionStatus.Failed
            execution.completedAt = LocalDateTime.now()
            executions.save(execution)
            throw RuntimeException("Workflow execution failed", e)
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
