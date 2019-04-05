package ng.upperlink.nibss.cmms.dto.emandates.subscriber;

import lombok.Data;


@Data
public class SubscriberRequest{

    private String email;

    private String payerName;

    private String payerAddress;

//    private String bankCode;

    private String accountNumber;

    private String phoneNumber;

    private String bvn;

    private String channelCode;

    private String password;


}
