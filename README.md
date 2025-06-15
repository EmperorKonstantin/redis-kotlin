[![progress-banner](https://backend.codecrafters.io/progress/redis/cef8d6b9-5f5a-48c1-8220-41a29c4a014c)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

This is a starting point for Kotlin solutions to the
["Build Your Own Redis" Challenge](https://codecrafters.io/challenges/redis).

In this challenge, you'll build a toy Redis clone that's capable of handling
basic commands like `PING`, `SET` and `GET`. Along the way we'll learn about
event loops, the Redis protocol and more.

**Note**: If you're viewing this repo on GitHub, head over to
[codecrafters.io](https://codecrafters.io) to try the challenge.

## Key Architectural Decisions:

1. **Coroutines over traditional threading** 
  - Enables handling thousands of concurrent connections efficiently
  - Leverages Kotlin's coroutines for asynchronous I/O operations
  - Enables non-blocking handling of client connections
  - Provides a lightweight concurrency model compared to threads
2. **ConcurrentHashMap for storage**
  - Provides thread-safe operations without explicit locking
3. **Volatile flag for lifecycle management**
  - Ensures proper visibility across threads during shutdown
4. **Structured concurrency**
  - Parent-child relationships between coroutines for proper cleanup

## Notable Implementation Details:

- The server follows Redis protocol conventions for response formats (+, -, :, *, $ prefixes)
- Pattern matching in KEYS command uses simple prefix/suffix matching rather than full glob patterns
- Error handling is comprehensive, preventing crashes while providing meaningful feedback
- The shutdown hook ensures graceful termination with statistics display

## Running the Code

1. Ensure you have `kotlin (>=2.0)` installed locally
2. Run `./amper run` to run your Redis server, which is implemented in
   `src/main/kotlin/Main.kt`.
3. Output will be streamed to your terminal.

## Test the Code

1. Ensure you have `kotlin (>=2.0)` installed locally
2. Run `./amper test` to run your tests, which are implemented in
   `test/MainTest.kt`.
3. Output will be streamed to your terminal.
