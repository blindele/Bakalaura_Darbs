package lv.grafiks.planotajs.controller;

import lv.grafiks.planotajs.dto.CreateEmployeeResponse;
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
    public CreateEmployeeResponse create(@RequestParam String email, @RequestBody Employee employee){
        return employeeService.save(employee,email);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        employeeService.delete(id);
    }

    @PostMapping("/{id}")
    public CreateEmployeeResponse update(@PathVariable Long id, @RequestParam String email ,@RequestBody Employee employee){
        employee.setId(id);
        return employeeService.save(employee,email);
    }

    @GetMapping("/pending-deactivation")
    public List<Employee> getPendingDeactivation(){
        return employeeService.getPendingDeactivation();
    }

    @PostMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id){
        employeeService.deactivate(id);
    }

}
