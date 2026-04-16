package lv.grafiks.planotajs.dto;

import lombok.Data;

@Data
public class GenerateScheduleRequest {
    private int year;
    private int month;
    private String shiftStart;
    private String shiftEnd;
    private int requiredEmployees;
}

