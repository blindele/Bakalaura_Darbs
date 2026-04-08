package lv.grafiks.planotajs.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "shift_start", nullable = false)
    private LocalDateTime start;

    @Column(name = "shift_end", nullable = false)
    private LocalDateTime end;

    public long getDurationHours() {
        return java.time.Duration.between(start, end).toHours();
    }
}