package ng.upperlink.nibss.cmms.dto.emandates.otp;

import lombok.Data;
import lombok.NoArgsConstructor;
import ng.upperlink.nibss.cmms.dto.emandates.AuthParam;

@Data
@NoArgsConstructor
public class ValidateOTP {
    private AuthParam authParam;
    private String sessionId;
    private String otp;
}
