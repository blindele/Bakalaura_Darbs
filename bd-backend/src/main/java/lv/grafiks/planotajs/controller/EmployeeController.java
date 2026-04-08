package lv.grafiks.planotajs.controller;

import lv.grafiks.planotajs.model.Employee;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.service.EmployeeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<Employee> getAll(){
        return employeeService.getAll();
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable Long id){
        return employeeService.getById(id);
    }

    @PostMapping
    public Employee create(@RequestBody Employee employee){
        return employeeService.save(employee);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        employeeService.delete(id);
    }

    @PostMapping("/{id}")
    public Employee update(@PathVariable Long id, @RequestBody Employee employee){
        employee.setId(id);
        return employeeService.save(employee);
    }

}
