package ng.upperlink.nibss.cmms.dto.emandates.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmandateDetails {
    private String domain;
    private String apiKey;
    private String username;
    private String notificationUrl;
//    private String clientPassKey;
}
