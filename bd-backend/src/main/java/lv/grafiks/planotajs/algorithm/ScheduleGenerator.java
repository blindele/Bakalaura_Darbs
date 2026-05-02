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
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

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
        int minPerDay = request.getMinEmployeesPerDay();


        MonthNorm norm = monthNormRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new RuntimeException("Mēneša norma nav atrasta"));

        List<Employee> allEmployees = employeeRepository.findAll();
        if (allEmployees.size() < requiredEmployees) {
            throw new RuntimeException("Nepietiekami darbinieku");
        }

        Collections.shuffle(allEmployees);
        List<Employee> selected = new ArrayList<>(allEmployees.subList(0, requiredEmployees));

        Schedule schedule = scheduleRepository.findByYearAndMonth(year, month)
                .orElse(new Schedule());
        schedule.setYear(year);
        schedule.setMonth(month);
        schedule.setPeriodStart(LocalDate.of(year, month, 1));
        schedule.setPeriodEnd(LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth()));
        schedule.setStatus(ScheduleStatus.GENERATED);
        schedule = scheduleRepository.save(schedule);

        shiftRepository.deleteAll(shiftRepository.findByScheduleId(schedule.getId()));

        long shiftHours = java.time.Duration.between(shiftStart, shiftEnd).toHours();
        int totalDays = LocalDate.of(year, month, 1).lengthOfMonth();
        int maxDayOff = requiredEmployees - minPerDay;

        int neededWorkDays = (int) Math.ceil((double) norm.getWorkingHours() / shiftHours);
        neededWorkDays = Math.min(neededWorkDays, totalDays);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(totalDays);

        List<LocalDate> allDates = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            allDates.add(d);
        }

        Map<Integer, List<LocalDate>> weekDays = new LinkedHashMap<>();
        for (LocalDate d : allDates) {
            int week = d.get(WeekFields.ISO.weekOfWeekBasedYear());
            weekDays.computeIfAbsent(week, k -> new ArrayList<>()).add(d);
        }

        Map<LocalDate, Integer> dayOffCount = new HashMap<>();
        for (LocalDate d : allDates) {
            dayOffCount.put(d, 0);
        }

        for (int i = 0; i < selected.size(); i++) {
            Employee employee = selected.get(i);
            boolean isMinor = !employee.isAdult(LocalDate.of(year, month, 1));

            if (isMinor && (shiftHours > 7 || shiftEnd.isAfter(LocalTime.of(22, 0)))) {
                continue;
            }

            Set<LocalDate> offDaySet = new LinkedHashSet<>();
            for (Map.Entry<Integer, List<LocalDate>> entry : weekDays.entrySet()) {
                List<LocalDate> week = entry.getValue();

                if (isMinor) {
                    for (LocalDate d : week) {
                        if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                            offDaySet.add(d);
                        }
                    }
                } else {
                    int off1 = i % week.size();
                    int off2 = (off1 + 1) % week.size();
                    for (int j = 0; j < week.size(); j++) {
                        if (j == off1 || j == off2) {
                            offDaySet.add(week.get(j));
                        }
                    }
                }

            }


        List<LocalDate> workDays = new ArrayList<>();
        for (LocalDate d : allDates) {
            if (offDaySet.contains(d) && dayOffCount.get(d) < maxDayOff) {
                dayOffCount.put(d, dayOffCount.get(d) + 1);
            } else {
                workDays.add(d);
            }
        }

        int currentWorkDays = workDays.size();
        if (currentWorkDays > neededWorkDays) {
            int extra = currentWorkDays - neededWorkDays;
            double step = (double) currentWorkDays / extra;
            List<LocalDate> trimmed = new ArrayList<>();

            for (int j = 0; j < workDays.size(); j++) {
                LocalDate day = workDays.get(j);
                boolean shouldSkip = false;
                for (int k = 0; k < extra; k++) {
                    if (j == (int) (k * step + step / 2)) {
                        shouldSkip = true;
                        break;
                    }
                }
                if (shouldSkip && dayOffCount.get(day) < maxDayOff) {
                    dayOffCount.put(day, dayOffCount.get(day) + 1);
                } else {
                    trimmed.add(day);
                }
            }
            workDays = trimmed;
        } else if (currentWorkDays < neededWorkDays) {
            int missing = neededWorkDays - currentWorkDays;
            List<LocalDate> offList = new ArrayList<>(offDaySet);
            double step = (double) offList.size() / missing;
            for (int k = 0; k < missing && k < offList.size(); k++) {
                LocalDate toAdd = offList.get((int) (k * step));
                workDays.add(toAdd);
                dayOffCount.put(toAdd, dayOffCount.get(toAdd) - 1);
            }
            Collections.sort(workDays);
        }


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
}