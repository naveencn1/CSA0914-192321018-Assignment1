# Task A1 – Design a File Format (Bus-Pass Records)

**Scenario:** about 8,000 passes per year. Fields: pass ID, register number, name, route number, boarding stop, validity date, amount paid.

## 1. Record layout 1 – delimited text file

| # | Field | Type / rule | Example |
|---|-------|-------------|---------|
| 1 | passId | `P` + digits, unique, ≤ 12 chars | `P1003` |
| 2 | registerNo | digits, ≤ 12 chars | `192321018` |
| 3 | name | text, ≤ 60 chars, must not contain `|` or newline | `V V Naveen Kumar` |
| 4 | routeNo | digits, ≤ 4 chars | `12` |
| 5 | boardingStop | text, ≤ 40 chars | `Sriperumbudur` |
| 6 | validUntil | ISO date `yyyy-MM-dd` | `2027-03-31` |
| 7 | amountPaid | integer rupees, ≥ 0 | `6500` |

* One record per line, fields separated by `|` (the pipe is chosen because names and stops never contain it, unlike the comma).
* UTF-8, LF line end, **no header line**, exactly **7 fields (6 delimiters)** per valid line.
* Example line: `P1003|192321020|Ravi|7|Tambaram|2027-03-31|6200`
* Size: ≈ 60 bytes/line (measured: 46 MB per 800,000 lines) × 8,000 ≈ **0.5 MB per year**.

## 2. Record layout 2 – binary file with `DataOutputStream`

File header (written once): `magic "BPAS"` (4 bytes) | `version` short (2) | `recordCount` int (4).

Each record is **framed** so that a damaged record can be skipped:

| Order | Item | Method | Bytes |
|-------|------|--------|-------|
| 1 | recordLength (bytes of payload) | `writeInt` | 4 |
| 2 | passId | `writeUTF` | 2 + len |
| 3 | registerNo | `writeUTF` | 2 + len |
| 4 | name | `writeUTF` | 2 + len |
| 5 | routeNo | `writeShort` | 2 |
| 6 | boardingStop | `writeUTF` | 2 + len |
| 7 | validUntil as yyyymmdd | `writeInt` | 4 |
| 8 | amountPaid | `writeInt` | 4 |
| 9 | CRC32 of fields 2–8 | `writeInt` | 4 |

Typical record ≈ 4 + 7 + 11 + 18 + 2 + 15 + 4 + 4 + 4 ≈ **69 bytes**, so ≈ 0.55 MB per year (a little larger than text because of the 2-byte length prefixes, record length and CRC). It is not human-readable; the CRC detects corruption.

**Fixed-length variant (for `RandomAccessFile`)** – every record is exactly **85 bytes**:
`status(1) | passId(8) | registerNo(12) | name(30) | routeNo(2) | stop(20) | validUntil int(4) | amount int(4) | crc(4)`, text padded with spaces/zero bytes.
Record *k* starts at byte `16 + k × 85` (16 = header). 8,000 records = 680,016 bytes.

## 3. Pseudocode (follows structured pseudocode: ALGORITHM / INPUT / OUTPUT / numbered steps)

```
ALGORITHM AddPass
INPUT   file F, record R = (passId, regNo, name, route, stop, validUntil, amount)
OUTPUT  SUCCESS or an error code
BEGIN
 1  IF Validate(R) = FALSE THEN RETURN ERROR_INVALID
 2  IF Find(F, R.passId) EXISTS THEN RETURN ERROR_DUPLICATE
 3  line  <- JOIN(R.passId, R.regNo, R.name, R.route, R.stop, R.validUntil, R.amount) WITH "|"
 4  OPEN F in APPEND mode
 5  WRITE line followed by NEWLINE through a buffered writer
 6  FLUSH the buffer
 7  FORCE the operating system to write the file to disk (sync)
 8  CLOSE F
 9  RETURN SUCCESS
END
```

```
ALGORITHM FindPass
INPUT   file F, key passId
OUTPUT  the matching record, or NULL
BEGIN
 1  IF F does not exist THEN RETURN NULL
 2  OPEN F for buffered reading
 3  WHILE a next line L exists DO
 4      R <- ParseLine(L)                  // returns NULL for a damaged line
 5      IF R = NULL THEN CONTINUE           // skip damaged line, never stop the search
 6      IF R.passId = passId THEN CLOSE F; RETURN R
 7  END WHILE
 8  CLOSE F
 9  RETURN NULL
END

ALGORITHM ParseLine
INPUT   line L
OUTPUT  record or NULL
BEGIN
 1  IF L is empty THEN RETURN NULL
 2  fields <- SPLIT(L, "|") keeping empty trailing fields
 3  IF COUNT(fields) <> 7 THEN RETURN NULL
 4  IF fields[1] is blank THEN RETURN NULL
 5  IF fields[7] is not an integer OR fields[7] < 0 THEN RETURN NULL
 6  RETURN record built from fields
END
```

```
ALGORITHM ListByRoute
INPUT   file F, routeNo
OUTPUT  list of records on that route, count of damaged lines
BEGIN
 1  result <- empty list ; damaged <- 0
 2  IF F does not exist THEN RETURN (result, 0)
 3  OPEN F for buffered reading
 4  WHILE a next line L exists DO
 5      R <- ParseLine(L)
 6      IF R = NULL THEN damaged <- damaged + 1 ; CONTINUE
 7      IF R.route = routeNo THEN ADD R to result
 8  END WHILE
 9  CLOSE F
10  RETURN (result, damaged)
END
```

## 4. Damaged data, missing file, incomplete write

| Problem | How it is detected | How it is handled |
|---------|--------------------|-------------------|
| **Damaged line** (wrong field count, non-numeric amount, empty line, blank key) | `ParseLine` checks field count = 7, numeric amount ≥ 0, non-blank key. Binary: length prefix does not match, or CRC32 mismatch. | Skip the line, count it, write it to a `rejects` log with its line number; searching continues. Never crash and never guess values. |
| **Missing file** | `FileNotFoundException` / `exists() = false` | *Reads:* return NULL / empty list (nothing stored yet). *Writes:* create the file, then append. Report clearly if the *directory* is missing or not writable. |
| **Incomplete write after power failure** | The last line has **no trailing newline**, or has < 7 fields; binary: recordLength larger than the bytes left in the file, or CRC mismatch. | Each add = one `write` of the whole line + newline, then `flush` and `sync`, so at most the **last** record can be torn. On start-up, check the tail: truncate (or quarantine) the partial final record. Whole-file rewrites use *write to `file.tmp`, sync, then atomic rename* so the old file is never half-replaced. |

## 5. Text file vs fixed-length records in `RandomAccessFile`

| Aspect | Delimited text | Fixed-length `RandomAccessFile` |
|--------|----------------|----------------------------------|
| Find by ID | scan, **O(n)** (≈ 75 ms for 800 k lines, measured) | **O(1)** if ID→slot is computable, otherwise O(log n) by binary search on a sorted file, still no parse of earlier records |
| Update one record | rewrite file (or append + tombstone) | `seek(16 + k×85)` then overwrite 85 bytes in place |
| Delete | rewrite file | write `status = 0` (tombstone) |
| Readability / debugging | open in any editor, Excel, `grep` | needs a tool |
| Space | variable, compact | padding wastes space (85 B vs ≈ 60 B) and fields have a hard maximum |
| Damage handling | damage confined to one line | damage confined to one slot, CRC detects it; a bad *length* cannot shift later records |
| Concurrency | none | none (needs `FileLock`) |

**Conclusion for 8,000 records/year:** the text file is simple and fast enough (scan ≈ ms). Fixed-length records pay off when records must be **updated in place or accessed by position** many times.
