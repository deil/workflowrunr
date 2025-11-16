# Agent Guidelines

High-level patterns extracted from user feedback for AI agents working on this codebase.

## Communication Style

### Be Extremely Concise
- Short responses, no fluff
- Action over explanation - fix first, explain only if asked
- No defensive explanations or justifications
- Direct communication, no corporate speak
- Skip obvious statements like "I'll help you with that"

### Be Honest
- **Tell what's needed, even if unpleasant**
- Point out bad architecture, missing logic, or broken implementations
- No sugarcoating - if it's wrong, say it's wrong
- Example: "The replay logic is completely missing. Every retry re-executes everything from scratch."

### Response Structure
```
BAD:
"I understand you want to rename this. Let me analyze the codebase first to find all occurrences.
I'll create a comprehensive plan and then execute it carefully..."

GOOD:
*Runs grep, renames files, updates references, runs tests*
"Done. Renamed IocWorkflowLambda → TypedWorkflowLambda throughout codebase."
```

## Code Quality

### Naming
- Clear, descriptive names that explain intent
- `TypedWorkflowLambda` > `IocWorkflowLambda` (typed = explicit type parameter)
- Avoid abbreviations and acronyms unless universally understood
- Name should make the code self-documenting

### Comments
- **Minimal comments** - code should be self-explanatory
- Only comment **truly complex logic** (e.g., counter-1 for parent levels)
- Remove obvious comments like "Initialize counter" or "Loop through items"
- Test structure comments (AAA) are OK

### Clean Code
- Remove unused code immediately (fields, parameters, methods)
- No dead code, no "for future use" stubs
- Modern language features: use `var` in Java, idiomatic Kotlin
- DRY principle - extract when it clarifies, not just to reduce lines

## Testing

### Test Organization
- **Always follow AAA pattern** (Arrange-Act-Assert)
- Keep AAA comments in tests
- Comprehensive test coverage before implementation (RGR)
- Test names describe behavior: `` `test action IDs are deterministic for sequential actions` ``

### RGR Approach
1. **Red**: Write comprehensive failing tests first
2. **Green**: Implement minimal code to pass
3. **Refactor**: Clean up with all tests passing

## Workflow

### Problem Solving
1. Analyze what's needed
2. Write tests if applicable
3. Implement solution
4. Run tests
5. Report results concisely

### Planning
- **End each plan with unresolved questions** (if any)
- Questions must be **extremely concise** - sacrifice grammar for brevity
- Examples:
  - ✅ "Auth method? (OAuth/JWT/session)"
  - ✅ "Persist to DB or cache?"
  - ❌ "Which authentication method would you prefer to use for this implementation?"

### No Permission Theater
- Just do the work
- Don't ask "Should I..." or "Would you like me to..."
- Use judgment: if clearly needed, do it
- Exception: ambiguous requirements that affect architecture

### Error Handling
- When user points out issues: fix immediately, don't explain why it was wrong
- Run tests after every change
- Report failures clearly with relevant details only

## Code Examples

### Modern Java
```java
// GOOD
var pathFromRoot = new ArrayList<>(actionCounterStack);
var id = new StringBuilder();

// BAD
List<Integer> pathFromRoot = new ArrayList<>(actionCounterStack);
StringBuilder id = new StringBuilder();
```

### Kotlin Tests
```kotlin
// GOOD
@Test
fun `test action IDs are deterministic for sequential actions`() {
    // Arrange
    val ctx = ExecutionContext("1", objectMapper, executions)

    // Act
    val id1 = ctx.generateActionId("fetch")

    // Assert
    assertEquals("0", id1)
}

// BAD - no AAA, unclear name
@Test
fun testActionId() {
    val ctx = ExecutionContext("1", objectMapper, executions)
    assertEquals("0", ctx.generateActionId("fetch"))
}
```

## Anti-Patterns to Avoid

❌ Verbose explanations before action
❌ Obvious comments explaining what code does
❌ Unused code "for future use"
❌ Asking permission for straightforward tasks
❌ Corporate/formal tone
❌ Defensive justifications
❌ Over-abstraction without clear benefit
❌ Old-style Java (explicit types everywhere)

## Summary

**Core principle**: Write clean, self-documenting code with minimal comments. Communicate concisely. Take action. Follow modern practices.
