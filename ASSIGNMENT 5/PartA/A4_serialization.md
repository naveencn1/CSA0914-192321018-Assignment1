# Task A4 – Serialization Design

Object graph: `Hostel` → `List<Room>` → each `Room` → `List<Resident>`; `Resident` has `name`, `registerNo`, `password`, and a live database `Connection`.

## 1. Transient fields

| Field | transient? | Reason |
|-------|-----------|--------|
| `Resident.connection` | **YES** | `Connection` is **not** `Serializable` → `NotSerializableException`. It is also a live operating-system/network resource: after the program ends the socket is closed, so a saved copy would be meaningless. Re-open on demand after reading. |
| `Resident.password` | **YES** | Security: serialized files are easily read; secrets must not be stored in the clear. The password lives in the database (as a salted hash) and is not part of the persistent object state. |
| `Resident.name`, `Resident.registerNo` | no | Real state that must be saved. |
| `Hostel.rooms`, `Room.residents` | no | Collections of serializable objects (`ArrayList`) are part of the state. |
Also never serialized: `static` fields (belong to the class). On reading, transient fields get default values (`null` / `0`).

## 2. What happens to the complete object graph
`writeObject(hostel)` performs a **depth-first traversal of everything reachable** from `hostel`:
1. `Hostel` and every `Room` and `Resident` must implement `Serializable` (otherwise `NotSerializableException`).
2. The stream writes class descriptors, then field values; non-transient reference fields are followed recursively.
3. Each object is written **once**; a second reference to the same object (e.g. one `Resident` listed in two collections) is written as a back-reference handle, so sharing and cycles (`Room` ↔ `Hostel`) are preserved.
4. `readObject` rebuilds the **whole graph** with the same structure; constructors of serializable classes are not called, transient fields are default values.
Cost: time and size grow with the **number of reachable objects**, O(n); saving one resident means saving the whole hostel.

## 3. Adding a phone-number field after old files were saved
* **With an explicit `private static final long serialVersionUID = 1L;` kept unchanged:** the change is *compatible*. Old files still load; the new `phone` field receives the default (`null`) and can be filled in later.
* **Without an explicit `serialVersionUID`:** Java computes one from the class structure; adding a field changes it, so old files fail with **`InvalidClassException`** (stream classdesc serialVersionUID mismatch) and the data is unreadable.
* Incompatible changes (changing a field's type, renaming/removing a class in the hierarchy) break old files even when the UID is kept.

## 4. Role of `serialVersionUID`; comparison with CSV/JSON
`serialVersionUID` is the version stamp written into the stream and compared on reading; equal stamps mean "I promise this class can read that data". Declare it explicitly and increase it only for deliberately incompatible changes.

| | Java serialization | CSV | JSON |
|---|---|---|---|
| Readable by humans / other languages | no, Java only | yes | yes |
| Nested object graphs, shared references | **yes**, automatic | no (flat) | nested, no shared refs |
| Schema evolution | fragile (UID rules) | add columns | tolerant (ignore unknown fields) |
| Size / speed | compact binary, fast for Java | compact | larger text |
| Security | **risky**: deserializing untrusted data can run attacker-chosen code (use `ObjectInputFilter`) | safe to parse | safe to parse |
| Partial read / search | must read the whole object | scan lines | parse whole document |
Measured (Task04, 800,000 passes): serialized 49 MB vs text 46 MB; a single lookup needs the **whole file deserialized (≈ 1.4 s)** vs ≈ 75 ms scan of the text file.
