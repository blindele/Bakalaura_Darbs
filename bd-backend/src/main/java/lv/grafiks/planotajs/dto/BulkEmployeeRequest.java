package lv.grafiks.planotajs.dto;


import lombok.Data;
import lv.grafiks.planotajs.model.Gender;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class BulkEmployeeRequest {

    private String name;
    private String surname;
    private String email;
    private LocalDate birthDate;
    private Gender gender;
    private boolean permanent = true;
    private Set<Integer> workingMonths = new HashSet<>();

}
