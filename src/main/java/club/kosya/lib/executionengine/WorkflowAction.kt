package club.kosya.lib.executionengine

import club.kosya.lib.workflow.ExecutionContext
import org.slf4j.LoggerFactory

class WorkflowAction(
    private val ctx: ExecutionContext,
    private val id: String,
    private val name: String,
) {
    fun <R> execute(lambda: Function0<R>): R {
        log.info("[{}] Action: {}", id, name)
        try {
            val result: R = lambda.invoke()
            log.info("[{}] Action result: {}", id, result)

            return result
        } catch (ex: Exception) {
            log.error("Exception while executing action: {}", ex.message, ex)
            throw RuntimeException(ex)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(WorkflowAction::class.java)
    }
}
