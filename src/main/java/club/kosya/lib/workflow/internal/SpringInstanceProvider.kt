package club.kosya.lib.workflow.internal

import club.kosya.lib.workflow.ServiceIdentifier
import club.kosya.lib.workflow.ServiceInstanceProvider
import org.springframework.context.ApplicationContext

class SpringInstanceProvider(private val applicationContext: ApplicationContext) : ServiceInstanceProvider {

    override fun getInstance(serviceIdentifier: ServiceIdentifier): Any {
        try {
            val serviceClass = Class.forName(serviceIdentifier.className)
            return applicationContext.getBean(serviceClass)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to resolve service: type=" + serviceIdentifier.className,
                e,
            )
        }
    }
}
