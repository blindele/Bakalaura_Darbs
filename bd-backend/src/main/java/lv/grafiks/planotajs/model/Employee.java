package lv.grafiks.planotajs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    @Size(min = 3, max = 12)
    private String name;

    @Column(nullable = false)
    @NotBlank
    @Size(min = 3, max = 15)
    private String surname;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private Gender gender;

    @NotNull
    @Past
    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private boolean permanent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "employee_working_months",
            joinColumns = @JoinColumn(name = "employee_id")
    )
    @Column(name = "month")
    private Set<Integer> workingMonths = new HashSet<>();


    @Column(nullable = false)
    private boolean active = true;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public boolean isAdult(LocalDate onDate) {
        return birthDate.plusYears(18).isEqual(onDate)
                || birthDate.plusYears(18).isBefore(onDate);
    }

    public boolean worksInMonth(int month){
        if(permanent) return true;
        return workingMonths.contains(month);
    }
}