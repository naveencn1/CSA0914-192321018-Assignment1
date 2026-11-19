# Task A3 – Data-Access Design Pattern

*(The original `StudentDB`, `CourseDB` and `MarksDB` sources were not supplied; the common structure below is the standard JDBC structure such classes share, with assumed table columns stated explicitly.)*

## 1. Common steps vs differing parts

| Common to all three classes (repeated code) | Different in each class |
|----------------------------------------------|-------------------------|
| 1. Obtain a `Connection` (URL, user, password) | Table name (`student`, `course`, `marks`) |
| 2. Create `PreparedStatement` from an SQL string | The SQL text and column list |
| 3. Bind parameters | Which fields are bound and their types |
| 4. Execute (`executeUpdate` / `executeQuery`) | – |
| 5. Loop over `ResultSet` | – |
| 6. **Map a row to an object** | Row → `Student` / `Course` / `Marks` mapping |
| 7. Handle `SQLException`, close resources (try-with-resources) | Primary key (`regNo`, `courseCode`, composite `(regNo, courseCode)`) |

## 2. Generic DAO interface
```
INTERFACE Dao<T, K>                         // T = entity type, K = primary-key type
    insert(entity T)         -> number of rows inserted
    findById(id K)           -> T or NOT_FOUND
    findAll()                -> list of T
    update(entity T)         -> number of rows updated
    delete(id K)             -> number of rows deleted
END INTERFACE
```

## 3. `StudentDao` implements `Dao<Student, String>`
```
CLASS StudentDao IMPLEMENTS Dao<Student, String>
    FIELD connection

    insert(s):
        ps <- PREPARE "INSERT INTO student(reg_no,name,department,year,email) VALUES (?,?,?,?,?)"
        BIND 1..5 <- s.regNo, s.name, s.department, s.year, s.email
        RETURN EXECUTE_UPDATE(ps)

    findById(regNo):
        ps <- PREPARE "SELECT reg_no,name,department,year,email FROM student WHERE reg_no = ?"
        BIND 1 <- regNo
        rs <- EXECUTE_QUERY(ps)
        IF rs HAS NEXT ROW THEN RETURN MapRow(rs) ELSE RETURN NOT_FOUND

    findAll():
        rs <- EXECUTE_QUERY "SELECT reg_no,name,department,year,email FROM student"
        list <- empty ; WHILE rs HAS NEXT ROW DO ADD MapRow(rs) TO list
        RETURN list

    update(s):  PREPARE "UPDATE student SET name=?,department=?,year=?,email=? WHERE reg_no=?" ; BIND ; EXECUTE_UPDATE
    delete(regNo): PREPARE "DELETE FROM student WHERE reg_no=?" ; BIND ; EXECUTE_UPDATE

    MapRow(rs): RETURN new Student(rs.reg_no, rs.name, rs.department, rs.year, rs.email)
END CLASS
```
`CourseDao` and `MarksDao` differ only in the SQL strings, the `MapRow` function and the key type (`MarksDao` uses a composite key object).

## 4. Object-to-table mapping (assumed fields)

| `Student` field | Column | SQL type | Note |
|-----------------|--------|----------|------|
| `regNo` | `reg_no` | `VARCHAR(12)` | **PRIMARY KEY** |
| `name` | `name` | `VARCHAR(60) NOT NULL` | |
| `department` | `department` | `VARCHAR(40)` | |
| `year` | `year` | `TINYINT` | |
| `email` | `email` | `VARCHAR(80)` | UNIQUE |

Primary key = `reg_no` (natural key, unique per student); `MarksDao` would use `(reg_no, course_code)` with foreign keys to `student` and `course`.

## 5. Generic DAO vs SQL written inside service/GUI classes

| | Generic DAO | SQL inside service / GUI |
|---|---|---|
| Duplication | one place for each SQL; shared helper for connection/mapping | same code copied into every screen |
| Change a column | edit one `MapRow` + SQL | search and edit every class |
| Testing | mock `Dao<T,K>` without a database | needs a live database |
| Replace database / add cache | new implementation of the interface | rewrite the whole application |
| Security | all SQL uses `PreparedStatement` in one reviewed place | each author may concatenate strings → SQL injection |
| Layering | GUI knows only objects | GUI knows tables, columns, `SQLException` |
Cost of the DAO: a few extra classes; worthwhile as soon as ≥ 2 classes touch the same table.
