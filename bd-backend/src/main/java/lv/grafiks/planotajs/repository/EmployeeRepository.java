package lv.grafiks.planotajs.repository;

import lv.grafiks.planotajs.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
