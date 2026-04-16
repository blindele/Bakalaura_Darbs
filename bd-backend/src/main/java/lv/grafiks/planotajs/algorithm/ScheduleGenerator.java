package lv.grafiks.planotajs.algorithm;


import lv.grafiks.planotajs.dto.GenerateScheduleRequest;
import lv.grafiks.planotajs.model.*;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.MonthNormRepository;
import lv.grafiks.planotajs.repository.ScheduleRepository;
import lv.grafiks.planotajs.repository.ShiftRepository;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
public class ScheduleGenerator {

    private final EmployeeRepository employeeRepository;
    private final MonthNormRepository monthNormRepository;
    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;

    public ScheduleGenerator(EmployeeRepository employeeRepository,
                             MonthNormRepository monthNormRepository,
                             ScheduleRepository scheduleRepository,
                             ShiftRepository shiftRepository) {
        this.employeeRepository = employeeRepository;
        this.monthNormRepository = monthNormRepository;
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
    }

    public Schedule generate(GenerateScheduleRequest request) {
        int year = request.getYear();
        int month = request.getMonth();
        LocalTime shiftStart = LocalTime.parse(request.getShiftStart());
        LocalTime shiftEnd = LocalTime.parse(request.getShiftEnd());
        int requiredEmployees = request.getRequiredEmployees();

        MonthNorm norm = monthNormRepository.findByYearAndMonth(year, month).orElseThrow(() -> new RuntimeException("Mēneša norma nav atrasta"));

        List<Employee> allEmployees = employeeRepository.findAll();
        if (allEmployees.size() < requiredEmployees) {
            throw new RuntimeException("Nepietiekami darbinieku");
        }
        Collections.shuffle(allEmployees);
        List<Employee> selected = allEmployees.subList(0, requiredEmployees);

        Schedule schedule = scheduleRepository.findByYearAndMonth(year, month).orElse(new Schedule());
        schedule.setYear(year);
        schedule.setMonth(month);
        schedule.setPeriodStart(LocalDate.of(year, month, 1));
        schedule.setPeriodEnd(LocalDate.of(year, month, 1).withDayOfMonth(
                LocalDate.of(year, month, 1).lengthOfMonth()));
        schedule.setStatus(ScheduleStatus.GENERATED);
        schedule = scheduleRepository.save(schedule);

        shiftRepository.deleteAll(shiftRepository.findByScheduleId(schedule.getId()));

        long shiftHours = java.time.Duration.between(shiftStart, shiftEnd).toHours();

        int offset = 0;
        for (Employee employee : selected) {
            boolean isMinor = !employee.isAdult(LocalDate.of(year, month, 1));

            if (isMinor && (shiftHours > 7 || shiftEnd.isAfter(LocalTime.of(22,0)))) {
                continue;
            }

            int neededWorkDays = (int) Math.ceil((double) norm.getWorkingHours() / shiftHours);
            int totalDays = LocalDate.of(year, month, 1).lengthOfMonth();
            neededWorkDays = Math.min(neededWorkDays, totalDays);

            List<LocalDate> workDays = greedyAssign(year, month, neededWorkDays, isMinor, offset);
            offset++;

            for (LocalDate day : workDays) {
                Shift shift = new Shift();
                shift.setEmployee(employee);
                shift.setSchedule(schedule);
                shift.setStart(LocalDateTime.of(day, shiftStart));
                shift.setEnd(LocalDateTime.of(day, shiftEnd));
                shiftRepository.save(shift);
            }
        }
        return schedule;
    }

    private List<LocalDate> greedyAssign(int year, int month, int neededWorkDays,
                                         boolean isMinor, int offset) {
        List<LocalDate> workDays = new ArrayList<>();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        Set<DayOfWeek> offDays;
        if (isMinor) {
            offDays = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        } else {
            List<DayOfWeek> allDays = List.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
            );
            int first = offset % 7;
            int second = (offset + 1) % 7;
            offDays = Set.of(allDays.get(first), allDays.get(second));
        }

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (workDays.size() >= neededWorkDays) break;
            if (!offDays.contains(day.getDayOfWeek())) {
                workDays.add(day);
            }
        }

        return workDays;
    }

}
