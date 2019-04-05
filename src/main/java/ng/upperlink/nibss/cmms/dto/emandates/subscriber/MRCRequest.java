package ng.upperlink.nibss.cmms.dto.emandates.subscriber;

import lombok.Data;
import lombok.NoArgsConstructor;
import ng.upperlink.nibss.cmms.dto.emandates.AuthParam;

@Data
@NoArgsConstructor
public class MRCRequest {
    private AuthParam authParam;
    private String mrc;
}
