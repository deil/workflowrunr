package club.kosya.lib.executionengine.internal

data class ExecutionFlow(
    val id: String = "",
    val actions: MutableList<ExecutedAction> = mutableListOf(),
) {
    constructor(id: String) : this(id, mutableListOf())
}

data class ExecutedAction(
    val id: String = "",
    var name: String? = null,
    var result: String? = null,
    var resultType: String? = null,
    var completed: Boolean = false,
    var wakeAt: java.time.Instant? = null,
    val childActions: MutableList<ExecutedAction> = mutableListOf(),
) {
    constructor(id: String) : this(id, null, null, null, false, null, mutableListOf())
}
