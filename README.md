# WorkflowRunR

> **Research Preview** - This is an active development project exploring workflow execution patterns. APIs and architecture may change significantly as the research evolves.

A workflow execution engine that enables complex, restartable processes with external dependencies.

## Core Purpose

WorkflowRunR allows developers to write workflows as regular Java/Kotlin code while providing automatic persistence, error tracking, and restartability. Think of it as giving your long-running processes the ability to survive crashes, restarts, and failures without losing progress.

## How It Works

At its heart, WorkflowRunR solves a fundamental problem: what happens when your complex multi-step process fails halfway through? Traditional approaches either restart everything from scratch or require complex manual state management. WorkflowRunR automatically tracks each step, preserves the state, and can resume exactly where it left off.

The magic happens through lambda serialization - the system analyzes your workflow code, extracts the method calls and parameters, and stores everything needed to reconstruct and resume the workflow later.

## Architecture

### Lambda Processing Library (`lib/lambda/`)

This is the brain of the system - it understands how to read your lambda expressions and turn them into something that can be stored and reconstructed.

**Public API:**
- `WorkflowLambda` - The main functional interface for defining workflows
- `TypedWorkflowLambda` - A type-safe version that explicitly specifies the bean class
- `LambdaSerializer` - Converts lambda expressions into serialized byte arrays
- `LambdaMethodInvocationParser` - Analyzes lambda bytecode to extract method calls
- `TypedLambdaMethodInvocationParser` - Handles typed lambdas with bean class extraction

**Internal Implementation (`lib/lambda/internal/`):**
- `BaseLambdaMethodInvocationParser` - The core ASM bytecode parsing logic
- `MethodInvocationInfo` - Data structure representing a method call
- `TypedMethodInvocationInfo` - Extended version that includes bean class information
- `ParameterSource` - Tracks whether parameters come from variables or constants

### Workflow Engine (`duraexec/`)

This is the runtime that actually executes your workflows and manages their lifecycle.

**Core Components:**
- `Workflow` - The main entry point for submitting and managing workflows
- `ExecutionContext` - Tracks workflow execution with unique hierarchical IDs
- `WorkflowAction` - Wraps individual steps with error handling and tracking
- `WorkflowDefinitionConverter` - Converts parsed lambdas into executable workflow definitions
- `WorkflowExecutor` - The engine that runs workflows and handles persistence

**Data Layer:**
- `Execution` - Database entity representing a workflow execution
- `ExecutionStatus` - States in the workflow lifecycle (Queued, Running, Failed, Completed)
- `ExecutionContextImpl` - Utility for running external processes like FFmpeg

## Two Ways to Write Workflows

### Instance Pattern (capture the bean)

```java
Workflow.run(() -> videoWorkflow.run(ctx, fileName))
```

In this pattern, you capture a specific bean instance in your lambda. The system analyzes the bytecode to figure out which bean and method you're calling. It's flexible and works well when you have a specific instance you want to use.

### Typed Pattern (specify the bean class)

```java
Workflow.<VideoWorkflow>run(x -> x.run(ctx, fileName))
```

Here you explicitly tell the system what type of bean you're using. The lambda parameter `x` becomes your bean instance. This approach is more type-safe and makes the intent clearer.

## Real-World Example

Here's how you might write a video processing workflow:

```java
public String run(ExecutionContext ctx, String videoFile) {
    // Step 1: Extract audio from video
    var audioFile = ctx.action("Extract audio track", () -> 
        extractAudio(videoFile)
    );
    
    // Step 2: Transcribe the audio to text
    var transcript = ctx.action("Transcribe audio to text", () -> 
        transcribeAudio(audioFile)
    );
    
    // Step 3: Generate summary
    return ctx.action("Generate summary", () -> 
        summarizeText(transcript)
    );
}
```

Each `action()` call creates a trackable step with a unique ID. If step 2 fails, the system knows exactly what failed and can restart from step 2 with all the context from step 1 preserved.

## What Makes This Powerful

**Hierarchical Tracking**: Every workflow execution gets a unique ID, and every action within that workflow gets its own sequential ID. This creates a clear hierarchy for debugging and monitoring.

**Error Isolation**: When something fails, you know exactly which step failed and why. No more digging through logs to figure out where your 10-step process went wrong.

**External Process Integration**: Seamlessly run command-line tools like FFmpeg, Whisper, or any other external process while maintaining the same tracking and restart capabilities.

**State Preservation**: All captured variables, method parameters, and intermediate results are automatically preserved. When a workflow restarts, it has access to everything it needs.

**Lambda Serialization**: The system can serialize your entire workflow - the method calls, the captured variables, everything - and store it in the database for later reconstruction.

## Technical Stack

- **Java 21** - Modern Java with latest language features
- **Spring Boot 3.5.4** - Used for development convenience only (API layer, database setup, dependency injection)
- **Kotlin** for some components - Where Kotlin makes the code cleaner and more concise
- **MySQL** for persistence with Flyway migrations - Reliable database with version-controlled schema
- **Docker Compose** for development - Easy local development setup
- **ASM** for lambda bytecode analysis - The library that makes lambda parsing possible

> **Note on Spring Boot**: The current implementation uses Spring Boot for development simplicity - it provides the API layer, database configuration, and dependency injection out of the box. However, the core workflow engine is designed to be framework-agnostic. Future versions will decouple from Spring Boot entirely, allowing integration with any DI framework or standalone usage.

## Getting Started

1. **Start the database**: `docker-compose up`
2. **Run the application**: `./gradlew bootRun`
3. **Submit a workflow**: `POST http://localhost:8080` with `{"file": "video.mp4"}`

The system takes care of everything else - queuing, execution, tracking, persistence, and restartability.

## Test Organization

The test suite follows a clear structure that mirrors the codebase:

```
src/test/java/
├── club/kosya/duraexec/
│   ├── ActionIdTest.kt                    # Tests action ID generation and hierarchy
│   ├── ResultPersistenceTest.kt             # Tests saving/loading workflow results
│   └── ResultTypeTest.kt                  # Tests type handling for results
└── club/kosya/lib/lambda/
    └── parse/
        ├── BasicLambdaTest.kt               # Core lambda parsing functionality
        ├── ExecutionContextPlaceholderTest.kt # Static field access in lambdas
        ├── JavaRecordTest.java              # Java record field accessor support
        └── MethodCallParameterTest.kt       # Method calls as lambda parameters
```

All tests follow the AAA pattern (Arrange-Act-Assert) with descriptive names that explain exactly what behavior is being tested.

## Current Status & Roadmap

This is a **research preview** demonstrating core workflow execution concepts. The implementation is actively evolving as we explore different patterns and approaches.

**What's Working:**
- Lambda serialization and bytecode analysis
- Two workflow patterns (instance and typed)
- Hierarchical action tracking
- Basic persistence and restart capability
- External process integration

**What's Being Researched:**
- More sophisticated error recovery strategies
- Distributed workflow execution
- Advanced parameter type handling
- Performance optimization for large workflows
- Integration patterns with popular frameworks

**Why This Matters**

In a world of microservices and distributed systems, long-running processes are inevitable. Video processing, data migrations, report generation, API orchestration - these are all workflows that need to be reliable and restartable.

WorkflowRunR provides the foundation for building these systems without having to reinvent state management, error tracking, and restart logic every time. Write your business logic once, and let WorkflowRunR handle the operational concerns.

---

*Note: This is research code, not production-ready software. Use for learning and experimentation, not for critical production workloads.*