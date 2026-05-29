package lv.grafiks.planotajs.dto;

import lombok.Data;

@Data
public class GenerateScheduleRequest {
    private int year;
    private int month;
    private String shiftStart;
    private String shiftEnd;
    private int requiredEmployees;
    private int minEmployeesPerDay;
    private String algorithmType = "Pirmais";


    public String getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(String algorithmType) {
        this.algorithmType = algorithmType;
    }

}

