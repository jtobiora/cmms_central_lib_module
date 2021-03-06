package ng.upperlink.nibss.cmms.dto.mandates;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MandateActionRequest {
    private Long mandateId;
    private String action;
}
