package lv.grafiks.planotajs.algorithm;

import lv.grafiks.planotajs.dto.GenerateScheduleRequest;
import lv.grafiks.planotajs.model.*;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.MonthNormRepository;
import lv.grafiks.planotajs.repository.ScheduleRepository;
import lv.grafiks.planotajs.repository.ShiftRepository;
import org.springframework.cglib.core.Local;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.time.*;
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

        long shiftHours = Duration.between(shiftStart, shiftEnd).toHours();
        LocalDate scheduleStart = LocalDate.of(year,month,1);

        LocalTime maxMinorEnd = LocalTime.of(22,0);
        LocalTime minorShiftEnd = shiftEnd.isAfter(maxMinorEnd) ? maxMinorEnd : shiftEnd;
        long minorPossibleHours = Duration.between(shiftStart, minorShiftEnd).toHours();
        LocalTime minorShiftStart = shiftStart;
        if(minorPossibleHours > 7) {
            minorShiftStart = minorShiftEnd.minusHours(7);
        }
        long minorShiftHours = Duration.between(minorShiftStart, minorShiftEnd).toHours();
        boolean minorCanWork  = minorShiftHours > 0;

        MonthNorm norm = monthNormRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new RuntimeException("Mēneša norma nav atrasta"));

        List<Employee> allEmployees = employeeRepository.findAll();
        List<Employee> eligibleEmployees = new ArrayList<>();
        for (Employee e : allEmployees) {
            if(!e.isActive()) continue;
            if(!e.worksInMonth(month)) continue;
            if(!e.isAdult(scheduleStart) && !minorCanWork) continue;
            eligibleEmployees.add(e);
        }

        if(eligibleEmployees.size() < requiredEmployees) {
            throw new RuntimeException(
                    "Nepietiekami darbinieki, kas atbilst prasībām"
            );
        }

        Collections.shuffle(eligibleEmployees);
        List<Employee> selected = new ArrayList<>(eligibleEmployees.subList(0, requiredEmployees));

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
            boolean isMinor = !employee.isAdult(scheduleStart);

            Set<LocalDate> offDaySet = new LinkedHashSet<>();

            if(isMinor) {
                for (LocalDate d : allDates) {
                    if (d.getDayOfWeek() == DayOfWeek.SATURDAY ||
                            d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        offDaySet.add(d);
                    }
                }
                } else {
                    for(Map.Entry<Integer, List<LocalDate>> entry : weekDays.entrySet()) {
                        List<LocalDate> week = entry.getValue();
                    int off1 = i % week.size();
                    int off2 = off1 + 1;
                    if(off2 >= week.size()){
                        off1--;
                        off2--;
                    }
                    for (int j = 0; j < week.size(); j++) {
                        if (j == off1 || j == off2) {
                            offDaySet.add(week.get(j));
                        }
                    }
                }

            }

        List<LocalDate> workDays = new ArrayList<>();
        for (LocalDate d : allDates) {
            if (offDaySet.contains(d)) {
                if(isMinor || dayOffCount.get(d) < maxDayOff) {
                    dayOffCount.put(d, dayOffCount.get(d) + 1);
                } else {
                    workDays.add(d);
                }
            } else {
                workDays.add(d);
            }
        }

        if(!isMinor) {
            Set<LocalDate> offs = new HashSet<>(allDates);
            offs.removeAll(workDays);
            int currentWorkDays = workDays.size();

            if (currentWorkDays > neededWorkDays) {
                int extra = currentWorkDays - neededWorkDays;
                int maxOff = 3;

                while (extra > 0) {
                    LocalDate day = null;
                    int sc = -1;
                    for (LocalDate d : workDays) {
                        if (dayOffCount.get(d) >= maxDayOff) continue;
                        int left = 0;
                        LocalDate prev = d.minusDays(1);
                        while(offs.contains(prev)) {
                            left++;
                            prev = prev.minusDays(1);
                        }
                        int right = 0;
                        LocalDate next = d.plusDays(1);
                        while(offs.contains(next)) {
                            right++;
                            next = next.plusDays(1);
                        }
                        if(left + 1 + right > maxOff) continue;

                        int s = 0;
                        if (offs.contains(d.minusDays(1))) s++;
                        if (offs.contains(d.plusDays(1))) s++;
                        if (s > sc) {
                            sc = s;
                            day = d;
                        }
                    }
                    if (day == null) break;
                    workDays.remove(day);
                    offs.add(day);
                    dayOffCount.put(day, dayOffCount.get(day) + 1);
                    extra--;
                }
            } else if (currentWorkDays < neededWorkDays) {
                int missing = neededWorkDays - currentWorkDays;
                while (missing > 0) {
                    LocalDate badOff = null;
                    int sc = Integer.MAX_VALUE;
                    for (LocalDate d : offs) {
                        int s = 0;
                        if (offs.contains(d.minusDays(1))) s++;
                        if (offs.contains(d.plusDays(1))) s++;
                        if (s < sc) {
                            sc = s;
                            badOff = d;
                        }
                    }
                    if (badOff == null) break;
                    workDays.add(badOff);
                    offs.remove(badOff);
                    dayOffCount.put(badOff, dayOffCount.get(badOff) - 1);
                    missing--;
                }
                Collections.sort(workDays);
            }
        }

        LocalTime actualStart = isMinor ? minorShiftStart : shiftStart;
        LocalTime actualEnd = isMinor ? minorShiftEnd : shiftEnd;


        for (LocalDate day : workDays) {
            Shift shift = new Shift();
            shift.setEmployee(employee);
            shift.setSchedule(schedule);
            shift.setStart(LocalDateTime.of(day, actualStart));
            shift.setEnd(LocalDateTime.of(day, actualEnd));
            shiftRepository.save(shift);
        }
    }
        return schedule;

    }
}