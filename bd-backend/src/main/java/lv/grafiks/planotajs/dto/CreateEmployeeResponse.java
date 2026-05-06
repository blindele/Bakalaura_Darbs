package lv.grafiks.planotajs.dto;


import lombok.Data;
import lv.grafiks.planotajs.model.Employee;

@Data
public class CreateEmployeeResponse {
    private Employee employee;
    private String email;
    private String password;

    public CreateEmployeeResponse(Employee employee, String email, String password) {
        this.employee = employee;
        this.email = email;
        this.password = password;
    }


}
