package lv.grafiks.planotajs.algorithm;

import lv.grafiks.planotajs.dto.GenerateScheduleRequest;
import lv.grafiks.planotajs.model.*;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.MonthNormRepository;
import lv.grafiks.planotajs.repository.ScheduleRepository;
import lv.grafiks.planotajs.repository.ShiftRepository;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;

@Component
public class ScheduleGenerator2 {

    private final EmployeeRepository employeeRepository;
    private final MonthNormRepository monthNormRepository;
    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;

    private static final int MAX_WORK_DAYS = 6;

    public ScheduleGenerator2(EmployeeRepository employeeRepository,
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

        long shiftHours = Duration.between(shiftStart, shiftEnd).toHours();
        LocalDate scheduleStart = LocalDate.of(year, month, 1);

        LocalTime maxMinorEnd = LocalTime.of(22,0);
        LocalTime minorShiftEnd = shiftEnd.isAfter(maxMinorEnd) ? maxMinorEnd : shiftEnd;
        long minorPossibleHours = Duration.between(shiftStart, minorShiftEnd ).toHours();
        LocalTime minorShiftStart = shiftStart;
        if (minorPossibleHours > 7) {
            minorShiftStart = minorShiftEnd.minusHours(7);
        }
        long minorShiftHours = Duration.between(minorShiftStart, minorShiftEnd).toHours();
        boolean minorCanWork = minorShiftHours > 0;

        MonthNorm norm = monthNormRepository.findByYearAndMonth(year,month)
                .orElseThrow(() -> new RuntimeException("Mēneša norma nav atrasta!"));

        List<Employee> allEmployees = employeeRepository.findAll();
        List<Employee> eligibleEmployees = new ArrayList<>();
        for (Employee e: allEmployees) {
            if(!e.isActive()) continue;
            if(!e.worksInMonth(month)) continue;
            if (!e.isAdult(scheduleStart) && !minorCanWork) continue;
            eligibleEmployees.add(e);
        }

        if (eligibleEmployees.size() < requiredEmployees) {
            throw new RuntimeException("Nepietiekami darbinieki");
        }

        Schedule schedule = scheduleRepository.findByYearAndMonth(year,month)
                .orElse(new Schedule());
        schedule.setYear(year);
        schedule.setMonth(month);
        schedule.setPeriodStart(scheduleStart);
        schedule.setPeriodEnd(scheduleStart.withDayOfMonth(scheduleStart.lengthOfMonth()));
        schedule.setStatus(ScheduleStatus.GENERATED);
        schedule = scheduleRepository.save(schedule);

        shiftRepository.deleteAll(shiftRepository.findByScheduleId(schedule.getId()));

        int totalDays = scheduleStart.lengthOfMonth();
        Map<Employee, Integer> targetDays = new HashMap<>();
        for (Employee e : eligibleEmployees) {
            boolean isMinor = !e.isAdult(scheduleStart);
            long hours = isMinor ? minorShiftHours : shiftHours;
            int target = (int) Math.ceil((double) norm.getWorkingHours() / hours);
            targetDays.put(e, Math.min(target, totalDays));
        }

        Map<Employee, Integer> workedDays = new HashMap<>();
        Map<Employee, Integer> daysInRow = new HashMap<>();
        Map<Employee, List<LocalDate>> assignedDays = new HashMap<>();
        for (Employee e : eligibleEmployees) {
            workedDays.put(e, 0);
            daysInRow.put(e, 0);
            assignedDays.put(e, new ArrayList<>());
        }

        for (int day = 1; day <= totalDays; day++) {
            LocalDate currentDate = LocalDate.of(year, month, day);
            DayOfWeek weekday = currentDate.getDayOfWeek();
            boolean isWeekend = weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY;

            List<Employee> candidates = new ArrayList<>();
            for (Employee e : eligibleEmployees) {
                boolean isMinor = !e.isAdult(scheduleStart);

                if (isMinor && isWeekend) continue;

                if(workedDays.get(e) >= targetDays.get(e)) continue;

                if(daysInRow.get(e) >= MAX_WORK_DAYS) continue;

                candidates.add(e);
            }

            candidates.sort((a,b) -> {
                int remainingA = targetDays.get(a) - workedDays.get(a);
                int remainingB = targetDays.get(b) - workedDays.get(b);
                if (remainingA != remainingB) {
                    return Integer.compare(remainingB, remainingA);
                }
                return Integer.compare(daysInRow.get(a), daysInRow.get(b));
            });



            int daysRemainingInMonth = totalDays - day + 1;
            Set<Employee> chosenEmployee = new LinkedHashSet<>();

            for ( Employee e : candidates) {
                int neededDays = targetDays.get(e) - workedDays.get(e);
                if (neededDays >= daysRemainingInMonth) {
                    chosenEmployee.add(e);
                }
            }

            for (Employee e : candidates) {
                if (chosenEmployee.size() >= requiredEmployees) break;
                chosenEmployee.add(e);
            }

            if (chosenEmployee.size() < requiredEmployees) {
                boolean someoneStillNeedsWork = false;
                for (Employee e : eligibleEmployees) {
                    if (workedDays.get(e) < targetDays.get(e)) {
                        someoneStillNeedsWork = true;
                        break;
                    }
                }
                if (someoneStillNeedsWork) {
                    throw new RuntimeException(
                            "Nav iespējams ģenerēt grafiku dienā " + currentDate +
                                    ": nav pietiekami daudz pieejamu darbinieku"
                    );
                }
            }

            for (Employee e : eligibleEmployees) {
                if (chosenEmployee.contains(e)) {
                    workedDays.put(e, workedDays.get(e) + 1);
                    daysInRow.put(e, daysInRow.get(e) + 1);
                    assignedDays.get(e).add(currentDate);
                } else {
                    daysInRow.put(e,0);
                }
            }
        }

        for (Employee e : eligibleEmployees) {
            boolean isMinor = !e.isAdult(scheduleStart);
            LocalTime actualStart = isMinor ? minorShiftStart : shiftStart;
            LocalTime actualEnd = isMinor ? minorShiftEnd : shiftEnd;

            for (LocalDate day : assignedDays.get(e)) {
                Shift shift = new Shift();
                shift.setEmployee(e);
                shift.setSchedule(schedule);
                shift.setStart(LocalDateTime.of(day, actualStart));
                shift.setEnd(LocalDateTime.of(day, actualEnd));
                shiftRepository.save(shift);
            }
        }

        return schedule;
    }

}
