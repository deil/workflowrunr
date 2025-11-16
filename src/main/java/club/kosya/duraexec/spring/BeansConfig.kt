package club.kosya.duraexec.spring

import club.kosya.lib.workflow.ServiceInstanceProvider
import club.kosya.lib.workflow.internal.SpringInstanceProvider
import club.kosya.lib.workflow.internal.WorkflowReconstructor
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
    fun serviceInstanceProvider(applicationContext: org.springframework.context.ApplicationContext): ServiceInstanceProvider =
        SpringInstanceProvider(applicationContext)

    @Bean
    fun workflowReconstructor(serviceInstanceProvider: ServiceInstanceProvider): WorkflowReconstructor = WorkflowReconstructor(serviceInstanceProvider)
}
