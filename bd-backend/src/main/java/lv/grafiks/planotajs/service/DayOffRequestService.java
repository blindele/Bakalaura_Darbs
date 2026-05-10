package lv.grafiks.planotajs.service;


import lv.grafiks.planotajs.model.DayOffRequest;
import lv.grafiks.planotajs.model.DayOffStatus;
import lv.grafiks.planotajs.model.Employee;
import lv.grafiks.planotajs.model.User;
import lv.grafiks.planotajs.repository.DayOffRequestRepository;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DayOffRequestService {

    private final DayOffRequestRepository dayOffRequestRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public DayOffRequestService(DayOffRequestRepository dayOffRequestRepository, UserRepository userRepository, EmployeeRepository employeeRepository) {
        this.dayOffRequestRepository = dayOffRequestRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    public DayOffRequest create(String email, LocalDate date) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Lietotājs nav atrasts"));

        Employee employee = user.getEmployee();
        if(employee == null) {
            throw new RuntimeException("Darbinieks nav atrasts");
        }

        DayOffRequest request = new DayOffRequest();
        request.setEmployee(employee);
        request.setDate(date);
        request.setStatus(DayOffStatus.PENDING);
        return dayOffRequestRepository.save(request);

    }

    public List<DayOffRequest> getMyRequests(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Darbinieks nav atrasts"));
        return dayOffRequestRepository.findByEmployeeId(user.getEmployee().getId());
    }

    public List<DayOffRequest> getPendingRequests() {
        return dayOffRequestRepository.findByStatus(DayOffStatus.PENDING);
    }

    public DayOffRequest updateStatus(Long id, DayOffStatus status) {
        DayOffRequest request = dayOffRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pieprasījums nav atrasts"));

        request.setStatus(status);
        return dayOffRequestRepository.save(request);
    }

}
