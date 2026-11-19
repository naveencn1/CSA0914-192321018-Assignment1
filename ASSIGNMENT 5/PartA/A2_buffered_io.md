# Task A2 – Buffered I/O

## 1. Prediction: byte-at-a-time reading of a 50 MB log
50 MB = 50 × 1,048,576 = **52,428,800 bytes** → one `read()` call per byte → **≈ 52.4 million calls** (+1 final call returning −1 at end of file).
*Measured in Task01 on a 50 MB test file (52,428,812 bytes): **52,428,813 `read()` calls**, 10,598 ms.*

**Why it is slow:** each `FileInputStream.read()` is a native call that crosses from Java into the operating system (system call: mode switch user→kernel, argument checks, copy of 1 byte, return). That fixed overhead (≈ 0.2 µs here, more on a loaded system) is paid 52 million times while the useful work (compare byte with `\n`) is trivial. The disk is not the bottleneck; the **call count** is.

## 2. Buffered redesign
```
ALGORITHM CountLinesBuffered
INPUT   file path P
OUTPUT  number of lines
BEGIN
 1  buffer <- new byte array of 65,536 elements
 2  lines  <- 0
 3  OPEN P for reading
 4  LOOP
 5      n <- READ up to 65,536 bytes into buffer      // one OS call per 64 KB
 6      IF n = -1 THEN EXIT LOOP
 7      FOR i FROM 0 TO n-1 DO
 8          IF buffer[i] = LINE_FEED THEN lines <- lines + 1
 9  END LOOP
10  CLOSE P
11  RETURN lines
END
```
(`BufferedReader.readLine()` or `BufferedInputStream` give the same effect with an internal 8 KB buffer.)
Prediction: 52,428,800 / 65,536 = **800 calls** (+1) → measured **802 calls, 18 ms**, i.e. **≈ 590× faster**.

## 3. How buffering reduces operating-system calls
The buffer is a block of memory inside the program. One `read(byte[])` asks the OS for a large chunk (64 KB); the following bytes are then served from memory by ordinary array indexing, with no system call. Calls fall from **N bytes to N / bufferSize**. Writing works the same way: many small `write()` calls collect in the buffer and go out as one block at `flush()`.

## 4. A situation where a buffer gives little or no benefit
* When the program **already reads in large blocks** (e.g. `read(byte[1 MB])`) – an extra buffer only copies the data twice.
* **Random access:** reading a few scattered bytes with `seek()` – a read-ahead buffer is filled and thrown away.
* **Tiny files** (smaller than the buffer) – one call either way.
* **Writes that must reach the disk immediately** (e.g. a log record followed by `flush()` + `sync()` for safety): the cost is the sync, not the call count.
* Interactive streams (keyboard, a socket) – the data arrives when it arrives.

## 5. Buffered reading vs `Files.readAllLines()`
| | Buffered (64 KB array / `BufferedReader`) | `Files.readAllLines()` |
|---|---|---|
| Time, 50 MB (measured) | **18 ms** (byte array) / 86 ms (`readLine`) | 157 ms (creates 878,594 `String` objects) |
| Memory | **O(1)**: fixed 64 KB buffer | **O(n)**: ≈ **96 MB** heap retained for the 50 MB file (measured), > file size because of object headers |
| Large files | works for any size | `OutOfMemoryError` for multi-GB files |
| Convenience | more code | one line; gives random access to lines |

Use readAllLines only when the whole file fits comfortably in memory *and* all lines are needed afterwards.
