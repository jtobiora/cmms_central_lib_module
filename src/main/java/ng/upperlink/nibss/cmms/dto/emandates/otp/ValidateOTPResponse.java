package ng.upperlink.nibss.cmms.dto.emandates.otp;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "ValidateOTPResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class ValidateOTPResponse
{
    @XmlElement(name = "MandateCode")
    private String mandateCode;

    @XmlElement(name = "TransType")
    private String transType;  /*Todo The transaction type is 1*/

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

    @XmlElement(name = "ReferenceNumber")
    private String referenceNumber;

    @XmlElement(name = "ResponseCode")
    private String responseCode;

    @XmlElement(name = "PaymentStatus")
    private String paymentStatus;

    @XmlElement(name = "CPayRef")
    private String cPayRef;

    @XmlElement(name = "HashValue")
    private String hashValue;
}