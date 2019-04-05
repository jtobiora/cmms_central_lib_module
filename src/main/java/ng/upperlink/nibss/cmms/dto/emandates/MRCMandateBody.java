package ng.upperlink.nibss.cmms.dto.emandates;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MRCMandateBody {
    private AuthParam authParam;
    private MRCMandateRequest mrcMandateRequest;
}
