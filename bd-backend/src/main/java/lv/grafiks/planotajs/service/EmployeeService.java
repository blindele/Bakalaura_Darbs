package lv.grafiks.planotajs.service;

import lv.grafiks.planotajs.dto.BulkEmployeeError;
import lv.grafiks.planotajs.dto.BulkEmployeeRequest;
import lv.grafiks.planotajs.dto.BulkEmployeeResponse;
import lv.grafiks.planotajs.dto.CreateEmployeeResponse;
import lv.grafiks.planotajs.model.Employee;
import lv.grafiks.planotajs.model.Role;
import lv.grafiks.planotajs.model.User;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
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

        if(!employee.isPermanent() &&
            (employee.getWorkingMonths() == null || employee.getWorkingMonths().isEmpty())) {
            throw new RuntimeException("Jānorāda vismaz viens mēnesis");
        }

        if (employee.getWorkingMonths() != null) {
            for (Integer month : employee.getWorkingMonths()) {
                if (month == null || month < 1 || month > 12) {
                    throw new RuntimeException("Mēneša vērtībai jābūt no 1 līdz 12");
                }
            }
        }

        if (employee.isPermanent()) {
            employee.getWorkingMonths().clear();
        }


        if(employee.getId() == null) {

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

        Employee saved = employeeRepository.save(employee);
        if (saved.getUser() != null) {
            saved.getUser().setEmail(email);
            userRepository.save(saved.getUser());
        }
        return new CreateEmployeeResponse(saved, email, null);
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

    public List<Employee> getPendingDeactivation() {
        int currentMonth = LocalDate.now().getMonthValue();
        return employeeRepository.findPendingDeactivation(currentMonth);
    }

    public void deactivate(Long id) {
        Employee employee = getById(id);

        if(!employee.isActive()) {
            throw new RuntimeException("Darbinieks jau ir deaktivizēts");
        }

        User user = employee.getUser();
        employee.setUser(null);
        employee.setActive(false);
        employeeRepository.save(employee);

        if(user != null){
            userRepository.delete(user);
        }

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

    public BulkEmployeeResponse saveBulk(List<BulkEmployeeRequest> requests) {
        List<CreateEmployeeResponse> successful = new ArrayList<>();
        List<BulkEmployeeError> failed = new ArrayList<>();

        for (BulkEmployeeRequest request : requests) {
            try {
                if (request.getName() == null || request.getName().isEmpty()) {
                    throw new RuntimeException("Vārds ir obligāts");
                }
                if (request.getSurname() == null || request.getSurname().isEmpty()) {
                    throw new RuntimeException("Uzvārds ir obligāts");
                }
                if (request.getEmail() == null || request.getEmail().isEmpty()) {
                    throw new RuntimeException("E-pasts ir obligāts");
                }
                if (request.getBirthDate() == null) {
                    throw new RuntimeException("Dzimšanas datums ir obligāts!");
                }
                if (request.getGender() == null) {
                    throw new RuntimeException("Dzimums ir obligāts");
                }

                Employee employee = new Employee();

                employee.setName(request.getName());
                employee.setSurname(request.getSurname());
                employee.setBirthDate(request.getBirthDate());
                employee.setGender(request.getGender());
                employee.setPermanent(request.isPermanent());
                employee.setWorkingMonths(request.getWorkingMonths() != null ? request.getWorkingMonths() : new HashSet<>());
                employee.setActive(true);

                successful.add(save(employee, request.getEmail()));
            } catch (Exception e) {
                failed.add(new BulkEmployeeError(
                        request.getName(),
                        request.getSurname(),
                        request.getEmail(),
                        e.getMessage()
                ));
            }
        }

        return new BulkEmployeeResponse(successful,failed);

    }

}
