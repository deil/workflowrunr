package club.kosya.lib.executionengine

import club.kosya.lib.deserialization.internal.ObjectDeserializerImpl
import club.kosya.lib.executionengine.internal.ExecutionContextImpl
import club.kosya.lib.executionengine.internal.ExecutionsRepository
import club.kosya.lib.workflow.ExecutionContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class ActionIdTest {
    private val objectMapper = ObjectMapper()
    private val executions = Mockito.mock(ExecutionsRepository::class.java)
    private val deserializer = ObjectDeserializerImpl(objectMapper)

    @Test
    fun `test action IDs are deterministic for sequential actions`() {
        // Arrange
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)

        // Act
        val id1 = ctx.generateActionId("fetch")
        val id2 = ctx.generateActionId("process")
        val id3 = ctx.generateActionId("save")

        // Assert
        Assertions.assertEquals("0", id1)
        Assertions.assertEquals("1", id2)
        Assertions.assertEquals("2", id3)
    }

    @Test
    fun `test same action name produces same ID at same position`() {
        // Arrange
        val ctx1 = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val ctx2 = ExecutionContextImpl("2", objectMapper, executions, deserializer)

        // Act
        val id1 = ctx1.generateActionId("fetch")
        val id2 = ctx2.generateActionId("fetch")

        // Assert
        Assertions.assertEquals(id1, id2, "Same action at same position should have same ID")
    }

    @Test
    fun `test different action names at same position produce same ID`() {
        // Arrange
        val ctx1 = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val ctx2 = ExecutionContextImpl("2", objectMapper, executions, deserializer)

        // Act
        val id1 = ctx1.generateActionId("fetch")
        val id2 = ctx2.generateActionId("process")

        // Assert
        Assertions.assertEquals(id1, id2, "Position matters, not name")
    }

    @Test
    fun `test nested actions have hierarchical IDs`() {
        // Arrange
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)

        // Act - simulate nesting
        val rootId = ctx.generateActionId("root")
        ctx.enterAction(rootId)

        val nestedId1 = ctx.generateActionId("nested1")
        val nestedId2 = ctx.generateActionId("nested2")

        ctx.exitAction()

        // Assert
        Assertions.assertEquals("0", rootId)
        Assertions.assertEquals("0.0", nestedId1)
        Assertions.assertEquals("0.1", nestedId2)
    }

    @Test
    fun `test deeply nested actions`() {
        // Arrange
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)

        // Act - simulate deep nesting: root -> level1 -> level2
        val rootId = ctx.generateActionId("root")
        ctx.enterAction(rootId)

        val level1Id = ctx.generateActionId("level1")
        ctx.enterAction(level1Id)

        val level2Id = ctx.generateActionId("level2")

        ctx.exitAction()
        ctx.exitAction()

        // Assert
        Assertions.assertEquals("0", rootId)
        Assertions.assertEquals("0.0", level1Id)
        Assertions.assertEquals("0.0.0", level2Id)
    }

    @Test
    fun `test multiple nested branches have independent counters`() {
        // Arrange
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)

        // Act - simulate: root -> (nested1, nested2), root2 -> (nested3)
        val root1 = ctx.generateActionId("root1")
        ctx.enterAction(root1)
        val nested1 = ctx.generateActionId("nested1")
        val nested2 = ctx.generateActionId("nested2")
        ctx.exitAction()

        val root2 = ctx.generateActionId("root2")
        ctx.enterAction(root2)
        val nested3 = ctx.generateActionId("nested3")
        ctx.exitAction()

        // Assert
        Assertions.assertEquals("0", root1)
        Assertions.assertEquals("0.0", nested1)
        Assertions.assertEquals("0.1", nested2)
        Assertions.assertEquals("1", root2)
        Assertions.assertEquals("1.0", nested3)
    }

    @Test
    fun `test complex nested structure`() {
        // Arrange
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)

        // Act - simulate complex tree:
        // root
        //   ├─ child1
        //   │    ├─ grandchild1
        //   │    └─ grandchild2
        //   └─ child2

        val root = ctx.generateActionId("root")
        ctx.enterAction(root)

        val child1 = ctx.generateActionId("child1")
        ctx.enterAction(child1)
        val grandchild1 = ctx.generateActionId("grandchild1")
        val grandchild2 = ctx.generateActionId("grandchild2")
        ctx.exitAction()

        val child2 = ctx.generateActionId("child2")
        ctx.exitAction()

        // Assert
        Assertions.assertEquals("0", root)
        Assertions.assertEquals("0.0", child1)
        Assertions.assertEquals("0.0.0", grandchild1)
        Assertions.assertEquals("0.0.1", grandchild2)
        Assertions.assertEquals("0.1", child2)
    }

    @Test
    fun `test action counter resets after exiting nested context`() {
        // Arrange
        val ctx = ExecutionContextImpl("1", objectMapper, executions, deserializer)

        // Act
        val action1 = ctx.generateActionId("action1")
        ctx.enterAction(action1)
        val nested1 = ctx.generateActionId("nested1")
        ctx.exitAction()

        val action2 = ctx.generateActionId("action2")
        ctx.enterAction(action2)
        val nested2 = ctx.generateActionId("nested2")
        ctx.exitAction()

        // Assert
        Assertions.assertEquals("0", action1)
        Assertions.assertEquals("0.0", nested1)
        Assertions.assertEquals("1", action2)
        Assertions.assertEquals("1.0", nested2, "Nested counter should reset for each parent")
    }

    @Test
    fun `test placeholder context throws on action execution`() {
        // Arrange
        val ctx = ExecutionContext.Placeholder

        // Act & Assert
        Assertions.assertThrows(UnsupportedOperationException::class.java) {
            ctx.await("test") { "result" }
        }
    }

    @Test
    fun `test action IDs are reproducible across context restarts`() {
        // Arrange - simulate first execution
        val ctx1 = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val ids1 = mutableListOf<String>()

        // Act - first execution
        ids1.add(ctx1.generateActionId("fetch"))
        ctx1.enterAction(ids1.last())
        ids1.add(ctx1.generateActionId("nested"))
        ctx1.exitAction()
        ids1.add(ctx1.generateActionId("save"))

        // Arrange - simulate replay
        val ctx2 = ExecutionContextImpl("1", objectMapper, executions, deserializer)
        val ids2 = mutableListOf<String>()

        // Act - replay with same structure
        ids2.add(ctx2.generateActionId("fetch"))
        ctx2.enterAction(ids2.last())
        ids2.add(ctx2.generateActionId("nested"))
        ctx2.exitAction()
        ids2.add(ctx2.generateActionId("save"))

        // Assert
        Assertions.assertEquals(ids1, ids2, "IDs must be identical across replays")
    }
}
