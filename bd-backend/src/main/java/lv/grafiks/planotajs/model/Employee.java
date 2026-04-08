package lv.grafiks.planotajs.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false)
    private LocalDate birthDate;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public boolean isAdult(LocalDate onDate) {
        return birthDate.plusYears(18).isEqual(onDate)
                || birthDate.plusYears(18).isBefore(onDate);
    }
}