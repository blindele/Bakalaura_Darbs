package lv.grafiks.planotajs.config;

import lv.grafiks.planotajs.model.*;
import lv.grafiks.planotajs.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final MonthNormRepository monthNormRepository;

    public DataInitializer(EmployeeRepository employeeRepository,
                           MonthNormRepository monthNormRepository) {
        this.employeeRepository = employeeRepository;
        this.monthNormRepository = monthNormRepository;
    }

    @Override
    public void run(String... args) {
        // Darbinieki
        if (employeeRepository.count() == 0) {
            Employee e1 = new Employee();
            e1.setName("Jānis");
            e1.setSurname("Bērziņš");
            e1.setBirthDate(LocalDate.of(2000, 5, 10));
            e1.setGender(Gender.MALE);
            employeeRepository.save(e1);

            Employee e2 = new Employee();
            e2.setName("Anna");
            e2.setSurname("Kalniņa");
            e2.setBirthDate(LocalDate.of(2008, 3, 15));
            e2.setGender(Gender.FEMALE);
            employeeRepository.save(e2);
        }

        // Mēneša normas 2026
        if (monthNormRepository.count() == 0) {
            int[][] norms = {
                    {1, 21, 168}, {2, 20, 160}, {3, 22, 176},
                    {4, 20, 158}, {5, 19, 152}, {6, 20, 159},
                    {7, 23, 184}, {8, 21, 168}, {9, 22, 176},
                    {10, 22, 176}, {11, 20, 159}, {12, 20, 158}
            };
            for (int[] n : norms) {
                MonthNorm norm = new MonthNorm();
                norm.setYear(2026);
                norm.setMonth(n[0]);
                norm.setWorkingDays(n[1]);
                norm.setWorkingHours(n[2]);
                monthNormRepository.save(norm);
            }
        }
    }
}