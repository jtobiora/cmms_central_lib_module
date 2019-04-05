package ng.upperlink.nibss.cmms.dto.emandates;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class PaymentStatusParam {
    private String requstDate;
    private String valueDate;
//    private String batchId;
    private String numberOfTrials;
    private String numberOfCreditTrials;
    private String status;
}
