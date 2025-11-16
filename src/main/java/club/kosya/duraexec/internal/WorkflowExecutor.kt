package club.kosya.duraexec.internal

import club.kosya.duraexec.ExecutionContext
import club.kosya.lib.workflow.BeanReference
import club.kosya.lib.workflow.WorkflowDefinition
import com.fasterxml.jackson.databind.ObjectMapper
import lombok.SneakyThrows
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.lang.reflect.Array
import java.time.LocalDateTime

@Component
class WorkflowExecutor(
    private val objectMapper: ObjectMapper,
    private val applicationContext: ApplicationContext,
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
                definition.beanReference.className,
                definition.methodName,
                definition.parameters,
            )

            val bean = resolveBean(definition.getBeanReference())

            val executionContext =
                ExecutionContext(
                    execution.id.toString(),
                    objectMapper,
                    executions,
                )

            val methodArgs = mutableListOf<Any?>()
            methodArgs.add(executionContext)
            methodArgs.addAll(definition.parameters)

            val result =
                invokeMethod(
                    bean,
                    definition.getMethodName(),
                    definition.getMethodDescriptor(),
                    methodArgs.toTypedArray(),
                )

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

    private fun resolveBean(beanReference: BeanReference): Any {
        try {
            val beanClass = Class.forName(beanReference.className)
            val bean: Any = applicationContext.getBean(beanClass)
            log.debug("Resolved bean by type: class={}", beanReference.className)
            return bean
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to resolve bean: type=" + beanReference.className,
                e,
            )
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(WorkflowExecutor::class.java)

        @SneakyThrows
        private fun invokeMethod(
            bean: Any,
            methodName: String,
            methodDescriptor: String,
            args: kotlin.Array<Any?>,
        ): Any? {
            // Parse method descriptor to get parameter types
            val paramTypes = parseParameterTypes(methodDescriptor)

            // Find method
            val method = bean.javaClass.getMethod(methodName, *paramTypes)
            method.setAccessible(true)

            log.debug(
                "Invoking method: {}#{} with {} arguments",
                bean.javaClass.getSimpleName(),
                methodName,
                args.size,
            )

            return method.invoke(bean, *args)
        }

        private fun parseParameterTypes(descriptor: String): kotlin.Array<Class<*>?> {
            val types: MutableList<Class<*>?> = ArrayList<Class<*>?>()

            // Parse descriptor format: (param1param2...)returnType
            var i = 1 // Skip opening '('
            while (descriptor.get(i) != ')') {
                var c = descriptor.get(i)

                // Handle array types
                var arrayDimensions = 0
                while (c == '[') {
                    arrayDimensions++
                    c = descriptor.get(++i)
                }

                val baseType: Class<*>

                // Handle object types
                if (c == 'L') {
                    val semicolon = descriptor.indexOf(';', i)
                    val className = descriptor.substring(i + 1, semicolon).replace('/', '.')
                    try {
                        baseType = Class.forName(className)
                    } catch (e: ClassNotFoundException) {
                        throw java.lang.RuntimeException("Class not found: " + className, e)
                    }
                    i = semicolon + 1
                } else {
                    baseType =
                        when (c) {
                            'Z' -> Boolean::class.javaPrimitiveType
                            'B' -> kotlin.Byte::class.javaPrimitiveType
                            'C' -> Char::class.javaPrimitiveType
                            'S' -> kotlin.Short::class.javaPrimitiveType
                            'I' -> kotlin.Int::class.javaPrimitiveType
                            'J' -> kotlin.Long::class.javaPrimitiveType
                            'F' -> kotlin.Float::class.javaPrimitiveType
                            'D' -> kotlin.Double::class.javaPrimitiveType
                            'V' -> Void.TYPE
                            else -> throw java.lang.IllegalArgumentException("Unknown type: " + c)
                        }!!
                    i++
                }

                var finalType = baseType
                for (j in 0..<arrayDimensions) {
                    finalType = Array.newInstance(finalType, 0).javaClass
                }

                types.add(finalType)
            }

            return types.toTypedArray<Class<*>?>()
        }
    }
}
