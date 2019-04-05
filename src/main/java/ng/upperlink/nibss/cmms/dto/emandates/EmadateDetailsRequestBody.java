package ng.upperlink.nibss.cmms.dto.emandates;

import lombok.Data;
import lombok.NoArgsConstructor;
import ng.upperlink.nibss.cmms.enums.makerchecker.EntityTypeEmandate;

import java.util.List;

@Data
@NoArgsConstructor
public class EmadateDetailsRequestBody {
    private Long objectId;
    private EntityTypeEmandate entityTypeEmandate;
    private List<String> emails;
}
