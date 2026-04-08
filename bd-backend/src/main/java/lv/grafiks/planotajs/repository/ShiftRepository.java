package lv.grafiks.planotajs.repository;

import lv.grafiks.planotajs.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByScheduleId(Long scheduleId);
    List<Shift> findByEmployeeId(Long employeeId);
}
