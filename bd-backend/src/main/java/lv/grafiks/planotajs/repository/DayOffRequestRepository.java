package lv.grafiks.planotajs.repository;

import lv.grafiks.planotajs.model.DayOffRequest;
import lv.grafiks.planotajs.model.DayOffStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DayOffRequestRepository extends JpaRepository<DayOffRequest, Long> {

    List<DayOffRequest> findByEmployeeId(Long employeeId);
    List<DayOffRequest> findByStatus(DayOffStatus status);

}
