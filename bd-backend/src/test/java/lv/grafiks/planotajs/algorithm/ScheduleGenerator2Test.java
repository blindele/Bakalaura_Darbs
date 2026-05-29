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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ScheduleGenerator2 algoritma testi")
class ScheduleGenerator2Test {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private MonthNormRepository monthNormRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ShiftRepository shiftRepository;

    @InjectMocks
    private ScheduleGenerator2 generator;

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
    @DisplayName("Katru dienu strādā vismaz requiredEmployees darbinieki")
    void everyDayHasAtLeastRequiredEmployees() {
        List<Employee> employees = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 5, 5));

        Map<LocalDate, Integer> employeesPerDay = new HashMap<>();
        for (Shift s : savedShifts) {
            employeesPerDay.merge(s.getStart().toLocalDate(), 1, Integer::sum);
        }

        LocalDate monthStart = LocalDate.of(2026, 9, 1);
        for (int d = 1; d <= 30; d++) {
            LocalDate date = monthStart.withDayOfMonth(d);
            int count = employeesPerDay.getOrDefault(date, 0);
            assertThat(count)
                    .as("Dienā " + date + " darbinieku skaits")
                    .isGreaterThanOrEqualTo(5);
        }
    }

    @Test
    @DisplayName("Darbinieks nestrādā vairāk par 6 dienām pēc kārtas")
    void noEmployeeWorksMoreThan6ConsecutiveDays() {
        List<Employee> employees = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 5, 5));

        Map<Long, Set<LocalDate>> workDaysPerEmp = new HashMap<>();
        for (Shift s : savedShifts) {
            workDaysPerEmp.computeIfAbsent(s.getEmployee().getId(), k -> new HashSet<>())
                    .add(s.getStart().toLocalDate());
        }

        LocalDate monthStart = LocalDate.of(2026, 9, 1);
        for (Long empId : workDaysPerEmp.keySet()) {
            Set<LocalDate> workDays = workDaysPerEmp.get(empId);
            int currentStreak = 0;
            int maxStreak = 0;
            for (int d = 1; d <= 30; d++) {
                LocalDate date = monthStart.withDayOfMonth(d);
                if (workDays.contains(date)) {
                    currentStreak++;
                    if (currentStreak > maxStreak) maxStreak = currentStreak;
                } else {
                    currentStreak = 0;
                }
            }
            assertThat(maxStreak)
                    .as("Darbinieks " + empId + " maks. darba dienu sērija")
                    .isLessThanOrEqualTo(6);
        }
    }

    @Test
    @DisplayName("nepilngadīgs darbinieks nestrādā sestdienās un svētdienās")
    void minorNoWeekendWork() {
        LocalDate minorBirth = LocalDate.of(2026, 9, 1).minusYears(17);
        Employee minor = makeEmployee(1L, "MinorEmp", minorBirth, true);

        List<Employee> employees = new ArrayList<>();
        employees.add(minor);
        for (long i = 2; i <= 20; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 5, 5));

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
    @DisplayName("Nepilngadīgo maiņa nepārsniedz 7 stundas")
    void minorShiftMax7Hours() {
        LocalDate minorBirth = LocalDate.of(2026, 9, 1).minusYears(17);
        Employee minor = makeEmployee(1L, "MinorEmp", minorBirth, true);

        List<Employee> employees = new ArrayList<>();
        employees.add(minor);
        for (long i = 2; i <= 20; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 5, 5));

        List<Shift> minorShifts = savedShifts.stream()
                .filter(s -> s.getEmployee().getId().equals(1L))
                .toList();

        for (Shift s : minorShifts) {
            long hours = Duration.between(s.getStart().toLocalTime(),
                    s.getEnd().toLocalTime()).toHours();
            assertThat(hours)
                    .as("Nepilngadīgo maiņas garums")
                    .isLessThanOrEqualTo(7L);
        }
    }

    @Test
    @DisplayName("Sezonas darbinieks neparādās ārpus saviem mēnešiem")
    void seasonalEmployeeNotInScheduleOutsideMonths() {
        Employee seasonal = makeEmployee(1L, "Seasonal", LocalDate.of(1990, 1, 1), false);
        seasonal.setWorkingMonths(new HashSet<>(Set.of(6)));

        List<Employee> employees = new ArrayList<>();
        employees.add(seasonal);
        for (long i = 2; i <= 21; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 5, 5));

        boolean seasonalInSchedule = savedShifts.stream()
                .anyMatch(s -> s.getEmployee().getId().equals(1L));

        assertThat(seasonalInSchedule).isFalse();
    }

    @Test
    @DisplayName("Neaktīvs darbinieks neparādās grafikā")
    void inactiveEmployeeNotInSchedule() {
        Employee inactive = makeEmployee(1L, "Inactive", LocalDate.of(1990, 1, 1), true);
        inactive.setActive(false);

        List<Employee> employees = new ArrayList<>();
        employees.add(inactive);
        for (long i = 2; i <= 21; i++) {
            employees.add(makeEmployee(i, "Emp" + i, LocalDate.of(1990, 1, 1), true));
        }
        when(employeeRepository.findAll()).thenReturn(employees);
        when(monthNormRepository.findByYearAndMonth(2026, 9))
                .thenReturn(Optional.of(makeNorm(2026, 9, 22, 176)));

        generator.generate(makeRequest(2026, 9, "11:00", "22:00", 5, 5));

        boolean inactiveInSchedule = savedShifts.stream()
                .anyMatch(s -> s.getEmployee().getId().equals(1L));

        assertThat(inactiveInSchedule).isFalse();
    }

    @Test
    @DisplayName("Algoritms izdod kļūdu, ja nepietiek darbinieku")
    void throwsWhenNotEnoughEmployees() {
        List<Employee> employees = new ArrayList<>();
        for (long i = 1; i <= 3; i++) {
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
}