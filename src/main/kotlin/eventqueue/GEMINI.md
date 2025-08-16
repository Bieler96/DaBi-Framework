# GEMINI.md

## Project Overview

This project implements a sophisticated, generic event queue in Kotlin. It is designed for asynchronous event processing with support for prioritization, timeouts, and batching. The event queue is built using Kotlin coroutines and provides a robust mechanism for handling events in a structured and reliable manner.

### Key Features:

*   **Prioritized Events:** Events can be enqueued with a specific priority, and the queue processes higher-priority events first.
*   **Timeouts:** Each event can have a timeout, and expired events are discarded.
*   **Batch Processing:** Events are processed in batches to improve efficiency.
*   **Dead-Letter Queue:** Events that fail processing after a certain number of retries are moved to a dead-letter queue for later inspection.
*   **Asynchronous Processing:** The event queue uses Kotlin coroutines for non-blocking, asynchronous event processing.
*   **Monitoring:** The queue tracks statistics such as the number of processed and discarded events, and the average processing time.

### Core Components:

*   **`EventQueue.kt`:** The main class that implements the event queue logic.
*   **`PrioritizedEvent.kt`:** A data class representing an event with its priority, timeout, and other metadata.
*   **`CriticalEvent.kt`:** An example of a specific event type that can be handled with a higher priority.
*   **`Main.kt`:** A sample application that demonstrates how to use the event queue.

## Building and Running

The build and run commands for this project are not defined in this directory. They are likely located in a parent directory.

**TODO:** Add the build and run commands here once they are identified.

## Development Conventions

*   **Kotlin Coroutines:** The project uses Kotlin coroutines for asynchronous operations. All I/O operations and long-running tasks should be performed within a coroutine to avoid blocking the main thread.
*   **Immutability:** The project uses immutable data classes to represent events and other data structures. This helps to ensure thread safety and makes the code easier to reason about.
*   **Error Handling:** The event queue includes a robust error handling mechanism with retries and a dead-letter queue. All event processing logic should handle exceptions gracefully and use the provided mechanisms for error reporting.
*   **Testing:** The project should include a comprehensive suite of unit tests to ensure the correctness of the event queue logic.

