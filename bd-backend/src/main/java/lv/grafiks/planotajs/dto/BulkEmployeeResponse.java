package lv.grafiks.planotajs.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkEmployeeResponse {

    private List<CreateEmployeeResponse> successful;
    private List<BulkEmployeeError> failed;

}
