package lv.grafiks.planotajs.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkEmployeeError {

    private String name;
    private String surname;
    private String email;
    private String error;
}
