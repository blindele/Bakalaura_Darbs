package lv.grafiks.planotajs.service;

import lv.grafiks.planotajs.dto.CreateEmployeeResponse;
import lv.grafiks.planotajs.model.Employee;
import lv.grafiks.planotajs.model.Role;
import lv.grafiks.planotajs.model.Shift;
import lv.grafiks.planotajs.model.User;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {

        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Employee> getAll(){
        return employeeRepository.findAll();
    }

    public Employee getById(Long id){

        Employee employee = employeeRepository.findById(id).orElse(null);
        if(employee == null){
            throw new RuntimeException("Darbinieks nav atrasts");
        }
        return employee;

    }

    public CreateEmployeeResponse save(Employee employee, String email) {

        String surname = employee.getSurname().length() >= 4
                ? employee.getSurname().substring(0, 4)
                : employee.getSurname();
        String year = String.valueOf(employee.getBirthDate().getYear());
        String rawPassword = surname + year;

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.EMPLOYEE);
        user = userRepository.save(user);

        employee.setUser(user);
        Employee saved = employeeRepository.save(employee);

        return new CreateEmployeeResponse(saved, email, rawPassword);

    }

    public void delete(Long id) {

        Employee employee = getById(id);
        User user  = employee.getUser();

        employee.setUser(null);
        employeeRepository.save(employee);

        if(employee.getUser() != null){
            userRepository.delete(employee.getUser());
        }
        employeeRepository.deleteById(id);

    }

    public Employee update(Employee employee, String email) {
        Employee existingEmployee = getById(employee.getId());

        existingEmployee.setName(employee.getName());
        existingEmployee.setSurname(employee.getSurname());
        existingEmployee.setBirthDate(employee.getBirthDate());
        existingEmployee.setGender(employee.getGender());

        if(existingEmployee.getUser() != null){
            User user = existingEmployee.getUser();
            user.setEmail(email);
            user = userRepository.save(user);
        }

        return employeeRepository.save(existingEmployee);

    }

}
