package ng.upperlink.nibss.cmms.dto.emandates.subscriber;

import lombok.Data;
import ng.upperlink.nibss.cmms.dto.emandates.AuthParam;
@Data
public class SubscriberRequestBody {
    private AuthParam authParam;
    private SubscriberRequest request;
}
