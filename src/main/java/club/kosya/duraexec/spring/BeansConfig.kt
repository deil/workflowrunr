package club.kosya.duraexec.spring

import club.kosya.lib.deserialization.ObjectDeserializer
import club.kosya.lib.deserialization.internal.ObjectDeserializerImpl
import club.kosya.lib.workflow.ServiceInstanceProvider
import club.kosya.lib.workflow.internal.SpringInstanceProvider
import club.kosya.lib.workflow.internal.WorkflowReconstructor
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@EnableScheduling
@Configuration
class BeansConfig {
    @Bean
    fun taskExecutor(): ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    @Bean
    fun serviceInstanceProvider(applicationContext: ApplicationContext): ServiceInstanceProvider =
        SpringInstanceProvider(applicationContext)

    @Bean
    fun objectDeserializer(objectMapper: ObjectMapper): ObjectDeserializer = ObjectDeserializerImpl(objectMapper)

    @Bean
    fun workflowReconstructor(
        serviceInstanceProvider: ServiceInstanceProvider,
        objectDeserializer: ObjectDeserializer,
    ): WorkflowReconstructor = WorkflowReconstructor(serviceInstanceProvider, objectDeserializer)
}
