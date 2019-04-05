package ng.upperlink.nibss.cmms.dto.emandates.otp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OTPResponse {

    String responseCode;
    String sessionId;
    String responseDescription;
}
