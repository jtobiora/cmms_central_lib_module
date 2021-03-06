package ng.upperlink.nibss.cmms.dto.emandates.otp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "GenerateOTPRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class GenerateOTPRequest {

    @XmlElement(name = "MandateCode")
    private String mandateCode;

    @XmlElement(name = "TransType")
    private String transType = "2"; /*Todo The transaction type is 2*/

    @XmlElement(name = "BankCode")
    private String bankCode;

    @XmlElement(name = "BillerID")
    private String billerId;

    @XmlElement(name = "BillerName")
    private String billerName;

    @XmlElement(name = "Amount")
    private String amount;

    @XmlElement(name = "BillerTransId")
    private String billerTransOd;

    @XmlElement(name = "HashValue")
    private String hashValue;
}
