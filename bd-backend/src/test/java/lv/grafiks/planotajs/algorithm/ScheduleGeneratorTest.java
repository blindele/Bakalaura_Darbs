package lv.grafiks.planotajs.algorithm;

import lv.grafiks.planotajs.dto.GenerateScheduleRequest;
import lv.grafiks.planotajs.model.*;
import lv.grafiks.planotajs.repository.EmployeeRepository;
import lv.grafiks.planotajs.repository.MonthNormRepository;
import lv.grafiks.planotajs.repository.ScheduleRepository;
import lv.grafiks.planotajs.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ScheduleGenerator algoritma testi")
class ScheduleGeneratorTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private MonthNormRepository monthNormRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ShiftRepository shiftRepository;

    @InjectMocks
    private ScheduleGenerator generator;

    private List<Shift> savedShifts;

    @BeforeEach
    void setUp() {
        savedShifts = new ArrayList<>();

        lenient().when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        lenient().when(scheduleRepository.findByYearAndMonth(anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        lenient().when(shiftRepository.save(any(Shift.class))).thenAnswer(inv -> {
            Shift s = inv.getArgument(0);
            savedShifts.add(s);
            return s;
        });

        lenient().when(shiftRepository.findByScheduleId(anyLong())).thenReturn(new ArrayList<>());
    }

    private Employee makeEmployee(Long id, String name, LocalDate birthDate, boolean permanent) {
        Employee e = new Employee();
        e.setId(id);
        e.setName(name);
        e.setSurname("Test");
        e.setBirthDate(birthDate);
        e.setGender(Gender.MALE);
        e.setPermanent(permanent);
        e.setActive(true);
        e.setWorkingMonths(new HashSet<>());
        return e;
    }

    private GenerateScheduleRequest makeRequest(int year, int month, String start, String end,
                                                int requiredEmployees, int minPerDay) {
        GenerateScheduleRequest r = new GenerateScheduleRequest();
        r.setYear(year);
        r.setMonth(month);
        r.setShiftStart(start);
        r.setShiftEnd(end);
        r.setRequiredEmployees(requiredEmployees);
        r.setMinEmployeesPerDay(minPerDay);
        return r;
    }

    private MonthNorm makeNorm(int year, int month, int workingDays, int workingHours) {
        MonthNorm n = new MonthNorm();
        n.setYear(year);
        n.setMonth(month);
        n.setWorkingDays(workingDays);
        n.setWorkingHours(workingHours);
        return n;
    }

    @Test
    @DisplayName("Darbinieks sasniedz mēneša normu")
    void adultEmployeeReachesMonthlyNorm() {
        List<Employee> employees = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 10, 5));

        Map<Long, Long> hoursPerEmployee = sumHoursPerEmployee();
        for (Long empId : hoursPerEmployee.keySet()) {
            long hours = hoursPerEmployee.get(empId);
            assertThat(hours)
                    .as("Darbinieks " + empId + " stundas")
                    .isBetween(165L, 187L);
        }
    }

    @Test
    @DisplayName("Nepilngadīgs darbinieks nesaņem garāku maiņu par 7 stundām")
    void minorEmployeeShiftNoLongerThan7Hours() {
        LocalDate minorBirth = LocalDate.of(2026, 9, 1).minusYears(17);
        Employee minor = makeEmployee(1L, "MinorEmp", minorBirth, true);

        List<Employee> employees = new ArrayList<>();
        employees.add(minor);
        for (long i = 2; i <= 10; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 10, 5));

        List<Shift> minorShifts = savedShifts.stream()
                .filter(s -> s.getEmployee().getId().equals(1L))
                .toList();

        assertThat(minorShifts).isNotEmpty();
        for (Shift s : minorShifts) {
            long hours = Duration.between(s.getStart().toLocalTime(),
                    s.getEnd().toLocalTime()).toHours();
            assertThat(hours)
                    .as("Nepilngadīgā maiņas garums dienā " + s.getStart().toLocalDate())
                    .isLessThanOrEqualTo(7L);
        }
    }

    @Test
    @DisplayName("Nepilngadīgs darbinieks nestrādā sestdienās un svētdienās")
    void minorEmployeeNoWeekendWork() {
        LocalDate minorBirth = LocalDate.of(2026, 9, 1).minusYears(17);
        Employee minor = makeEmployee(1L, "MinorEmp", minorBirth, true);

        List<Employee> employees = new ArrayList<>();
        employees.add(minor);
        for (long i = 2; i <= 10; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 10, 5));

        List<Shift> minorShifts = savedShifts.stream()
                .filter(s -> s.getEmployee().getId().equals(1L))
                .toList();

        for (Shift s : minorShifts) {
            java.time.DayOfWeek dow = s.getStart().getDayOfWeek();
            assertThat(dow)
                    .as("Nepilngadīgie nedrīkst strādāt " + dow)
                    .isNotIn(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY);
        }
    }

    @Test
    @DisplayName("Nepilngadīgs darbinieks nesaņem maiņu, kas beidzas pēc 22:00")
    void minorEmployeeNoShiftAfter22() {
        LocalDate minorBirth = LocalDate.of(2026, 9, 1).minusYears(17);
        Employee minor = makeEmployee(1L, "MinorEmp", minorBirth, true);

        List<Employee> employees = new ArrayList<>();
        employees.add(minor);
        for (long i = 2; i <= 10; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "14:00", "23:00", 10, 5));

        List<Shift> minorShifts = savedShifts.stream()
                .filter(s -> s.getEmployee().getId().equals(1L))
                .toList();

        for (Shift s : minorShifts) {
            assertThat(s.getEnd().toLocalTime())
                    .as("Nepilngadīgā maiņas beigas")
                    .isBeforeOrEqualTo(LocalTime.of(22, 0));
        }
    }

    @Test
    @DisplayName("Sezonas darbinieks neparādās grafikā ārpus saviem darba mēnešiem")
    void seasonalEmployeeNotInScheduleOutsideWorkingMonths() {
        Employee seasonal = makeEmployee(1L, "Seasonal", LocalDate.of(1990, 1, 1), false);
        seasonal.setWorkingMonths(new HashSet<>(Set.of(6)));

        List<Employee> employees = new ArrayList<>();
        employees.add(seasonal);
        for (long i = 2; i <= 12; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 10, 5));

        boolean seasonalInSchedule = savedShifts.stream()
                .anyMatch(s -> s.getEmployee().getId().equals(1L));

        assertThat(seasonalInSchedule)
                .as("Sezonas darbinieks septembrī nedrīkst būt grafikā")
                .isFalse();
    }

    @Test
    @DisplayName("Neaktīvs darbinieks neparādās grafikā")
    void inactiveEmployeeNotInSchedule() {
        Employee inactive = makeEmployee(1L, "Inactive", LocalDate.of(1990, 1, 1), true);
        inactive.setActive(false);

        List<Employee> employees = new ArrayList<>();
        employees.add(inactive);
        for (long i = 2; i <= 12; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 10, 5));

        boolean inactiveInSchedule = savedShifts.stream()
                .anyMatch(s -> s.getEmployee().getId().equals(1L));

        assertThat(inactiveInSchedule)
                .as("Neaktīvs darbinieks nedrīkst būt grafikā")
                .isFalse();
    }

    @Test
    @DisplayName("Algoritms izdod kļūdu, ja nepietiek darbinieku")
    void throwsExceptionWhenNotEnoughEmployees() {
        List<Employee> employees = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        lenient().when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        assertThatThrownBy(() ->
                generator.generate(makeRequest(2026, 9, "11:00", "22:00", 10, 5))
        )
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Algoritms izdod kļūdu, ja mēneša norma nav atrasta")
    void throwsExceptionWhenMonthNormMissing() {
        List<Employee> employees = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        lenient().when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                generator.generate(makeRequest(2026, 9, "11:00", "22:00", 10, 5))
        )
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Maksimālās brīvdienas nepārsniedz 4 dienas pēc kārtas")
    void offDayStreakDoesNotExceed4Days() {
        List<Employee> employees = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 10, 5));

        Map<Long, Set<LocalDate>> workDaysPerEmp = new HashMap<>();
        for (Shift s : savedShifts) {
            workDaysPerEmp.computeIfAbsent(s.getEmployee().getId(), k -> new HashSet<>())
                    .add(s.getStart().toLocalDate());
        }

        LocalDate monthStart = LocalDate.of(2026, 9, 1);
        int monthLength = monthStart.lengthOfMonth();

        for (Long empId : workDaysPerEmp.keySet()) {
            Set<LocalDate> workDays = workDaysPerEmp.get(empId);
            int currentStreak = 0;
            int maxStreak = 0;
            for (int d = 1; d <= monthLength; d++) {
                LocalDate date = monthStart.withDayOfMonth(d);
                if (workDays.contains(date)) {
                    currentStreak = 0;
                } else {
                    currentStreak++;
                    if (currentStreak > maxStreak) maxStreak = currentStreak;
                }
            }
            assertThat(maxStreak)
                    .as("Darbinieks " + empId + " maksimālā brīvdienas")
                    .isLessThanOrEqualTo(4);
        }
    }

    private Map<Long, Long> sumHoursPerEmployee() {
        Map<Long, Long> result = new HashMap<>();
        for (Shift s : savedShifts) {
            long hours = Duration.between(s.getStart().toLocalTime(),
                    s.getEnd().toLocalTime()).toHours();
            result.merge(s.getEmployee().getId(), hours, Long::sum);
        }
        return result;
    }
}