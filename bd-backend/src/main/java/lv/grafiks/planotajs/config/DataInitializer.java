package lv.grafiks.planotajs.config;

import lv.grafiks.planotajs.model.*;
import lv.grafiks.planotajs.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final MonthNormRepository monthNormRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public DataInitializer(EmployeeRepository employeeRepository,
                           MonthNormRepository monthNormRepository,
                           PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.monthNormRepository = monthNormRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        // Darbinieki
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

        if(userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail("admin@ventspils.lv");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

    }
}