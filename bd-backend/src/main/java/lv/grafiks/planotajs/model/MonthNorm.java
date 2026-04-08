package lv.grafiks.planotajs.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "month_norms")
public class MonthNorm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int workingDays;

    @Column(nullable = false)
    private int workingHours;
}