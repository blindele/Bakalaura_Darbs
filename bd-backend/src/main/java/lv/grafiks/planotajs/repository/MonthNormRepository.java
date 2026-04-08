package lv.grafiks.planotajs.repository;

import lv.grafiks.planotajs.model.MonthNorm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthNormRepository extends JpaRepository<MonthNorm, Long> {
    Optional<MonthNorm> findByYearAndMonth(int year, int month);
}
