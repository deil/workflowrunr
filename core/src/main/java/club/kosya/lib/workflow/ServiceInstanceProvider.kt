package club.kosya.lib.workflow

fun interface ServiceInstanceProvider {
    fun getInstance(serviceIdentifier: ServiceIdentifier): Any
}
