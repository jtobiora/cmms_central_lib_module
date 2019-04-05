package ng.upperlink.nibss.cmms.dto.emandates;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmandateRequest implements Serializable{
    private String accountNumber;

    private String phoneNumber;

    private String payerName;

    private String payerAddress;

    private String narration;

    private Long productId;

    private String emailAddress;

    private String startDate;

    private String endDate;

    private String channelCode;

//    private String subscriberPassCode;

    private String subscriberCode;

     private String bankCode;

    private boolean fixedAmountMandate;

    private BigDecimal amount;

    private Integer frequency;
}
