package lv.grafiks.planotajs.service;

import lv.grafiks.planotajs.model.Employee;
import lv.grafiks.planotajs.model.Shift;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {
    private EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<Employee> getAll(){
        return employeeRepository.findAll();
    }

    public Employee getById(Long id){
        return employeeRepository.findById(id).get()
    }

    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

}
