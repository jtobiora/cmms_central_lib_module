package ng.upperlink.nibss.cmms.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndustrySearchRequest {
    private String name;
    private String description;
}
