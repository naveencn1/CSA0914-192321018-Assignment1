package campus.dao;

import campus.model.EventInfo;
import campus.model.Registration;
import java.util.List;

/** Data-access layer contract. eventId == null means "all events". */
public interface RegistrationDao {
    List<EventInfo> findEvents();
    boolean exists(String studentId, int eventId);
    /** @throws DuplicateKeyException if the unique (student, event) rule is broken at the database level */
    void insert(Registration r);
    List<Registration> findPage(Integer eventId, int limit, int offset);
    List<Registration> findAll(Integer eventId);
    int count(Integer eventId);
}
