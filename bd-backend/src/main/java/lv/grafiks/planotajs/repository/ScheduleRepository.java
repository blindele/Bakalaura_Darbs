package lv.grafiks.planotajs.repository;

import lv.grafiks.planotajs.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    Optional<Schedule> findByYearAndMonth(int year, int month);
}
