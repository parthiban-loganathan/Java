# Experiment 7: Synchronizing Producer and Consumer Threads with a Shared Message Buffer

## Problem statement (syllabus)
A shared resource, a `MessageBuffer`, holds a single message and is managed by two threads: a `Producer` and a `Consumer`. Implement inter-thread communication using `wait()`, `notify()`, and `synchronized` blocks so that:
- The `Consumer` waits if the buffer is empty.
- The `Producer` waits if the buffer is full.
- The `Producer` notifies the `Consumer` when a new message is available.
- The `Consumer` notifies the `Producer` after consuming a message.

### Civil engineering analogy: Ready-Mix Concrete Hopper Synchronization

Imagine a concrete batching plant (`Producer` thread) and a transit mixer truck (`Consumer` thread) sharing a single loading hopper (`MessageBuffer`):
- **Thread (`extends Thread`):** A separate worker operating simultaneously on the site. Calling `start()` starts the worker's `run()` method.
- **`synchronized`:** A lock on the hopper gate ensuring the plant and truck never access the hopper at the exact same microsecond.
- **`wait()`:** Tells the current worker to pause and wait if the hopper isn't ready (full for Producer, empty for Consumer).
- **`notify()`:** Rings a bell to wake up the other waiting worker once the hopper state changes.

## Sample input and expected output

These cases are checked automatically on the JDK by `tools/CourseCheck.java`.

**Case 1**

Input:

```text
(no input)
```

Output:

```text
Produced: Message 1
Consumed: Message 1
Produced: Message 2
Consumed: Message 2
Produced: Message 3
Consumed: Message 3
Produced: Message 4
Consumed: Message 4
Produced: Message 5
Consumed: Message 5
```

## Notes

The output is deterministic: every print happens inside a `synchronized` method, and the `isEmpty` flag forces strict alternation, so `Produced: Message n` is always followed by `Consumed: Message n`.

## Run on your computer

```bash
javac Main.java
java Main < input.txt
```

Or type the input yourself after `java Main`.
