package lv.grafiks.planotajs.repository;

import lv.grafiks.planotajs.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Long id(Long id);

    List<Employee> findAllByActiveTrue();

    @Query("""
            SELECT e from Employee e
            WHERE e.active = true
            AND e.permanent = false
            AND NOT EXISTS (
                SELECT m FROM Employee e2 JOIN e2.workingMonths m
                WHERE e2.id = e.id AND m >= :currentMonth
            )
            
""")
    List<Employee> findPendingDeactivation(@Param("currentMonth") int currentMonth);
}
