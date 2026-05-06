package lv.grafiks.planotajs.controller;

import lv.grafiks.planotajs.algorithm.ScheduleGenerator;
import lv.grafiks.planotajs.dto.GenerateScheduleRequest;
import lv.grafiks.planotajs.model.Employee;
import lv.grafiks.planotajs.model.Schedule;
import lv.grafiks.planotajs.model.Shift;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.ScheduleRepository;
import lv.grafiks.planotajs.repository.ShiftRepository;
import lv.grafiks.planotajs.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final EmployeeRepository employeeRepository;
    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final ScheduleGenerator scheduleGenerator;
    private ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService, EmployeeRepository employeeRepository, ScheduleRepository scheduleRepository, ShiftRepository shiftRepository, ScheduleGenerator scheduleGenerator) {
        this.scheduleService = scheduleService;
        this.employeeRepository = employeeRepository;
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
        this.scheduleGenerator = scheduleGenerator;
    }

    @PostMapping
    public Schedule create(@RequestBody Schedule schedule) {
        return scheduleService.save(schedule);
    }

    @GetMapping("/{id}")
    public Schedule getById(@PathVariable long id) {
        return scheduleService.getById(id);
    }

    @GetMapping("/{year}/{month}")
    public Schedule getByYearAndMonth(@PathVariable int year, @PathVariable int month) {
        return scheduleService.getByYearAndMonth(year, month);
    }

    @GetMapping("/{id}/shifts")
    public List<Shift> getShifts(@PathVariable long id) {
        return scheduleService.getShiftsBySchedule(id);
    }

    @PostMapping("/shifts")
    public Shift createShift(@RequestBody Shift shift) {
        return scheduleService.saveShift(shift);
    }

    @DeleteMapping("/shifts/{id}")
    public void deleteShift(@PathVariable long id) {
        scheduleService.deleteShift(id);
    }

    @PutMapping("/shifts/{id}")
    public Shift updateShift(@PathVariable long id, @RequestBody Shift shift) {
        shift.setId(id);
        return scheduleService.saveShift(shift);
    }


    @GetMapping("/{year}/{month}/view")
    public Map<String, Object> getScheduleView(@PathVariable int year, @PathVariable int month) {

        List<Employee> employees = employeeRepository.findAll();

        List<String> days = new ArrayList<>();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            days.add(d.toString());
        }

        Optional<Schedule> scheduleOpt = scheduleRepository.findByYearAndMonth(year, month);
        if (scheduleOpt.isEmpty()) {
            return Map.of("employees", employees, "days", days, "shifts", List.of());
        }

        Schedule schedule = scheduleOpt.get();
        List<Shift> shifts = shiftRepository.findByScheduleId(schedule.getId());

        return Map.of("employees", employees, "days", days, "shifts", shifts, "scheduleId", schedule.getId());
    }

    @PostMapping("/generate")
    public Schedule generate(@RequestBody GenerateScheduleRequest request) {
        return scheduleGenerator.generate(request);
    }



}
