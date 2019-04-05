package ng.upperlink.nibss.cmms.dto.emandates.subscriber;

import lombok.Data;

import java.io.Serializable;

@Data
public class SubscriberResponse implements Serializable {

    private String payerName;
    private String email;
    private String phoneNumber;
    private String address;
    private String bankName;
    private String accountNumber;
    private String bvn;
    private String mandateReferenceCode;
    private String status;

}
