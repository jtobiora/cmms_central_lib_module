package ng.upperlink.nibss.cmms.dto.emandates;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class MRCMandateRequest{
        private String mandateReferenceCode;
        private String mrcPin;
        private String narration;

        private Long productId;

        private String startDate;

        private String endDate;

        private String channelCode;

//        private String subscriberPassCode;

        private String subscriberCode;

        private boolean fixedAmountMandate;

        private BigDecimal amount;

        private Integer frequency;
}
