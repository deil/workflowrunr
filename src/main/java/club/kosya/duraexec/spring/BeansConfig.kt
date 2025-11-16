package club.kosya.duraexec.spring

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
}
