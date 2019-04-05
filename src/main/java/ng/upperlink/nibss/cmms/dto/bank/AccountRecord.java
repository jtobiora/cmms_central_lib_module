package ng.upperlink.nibss.cmms.dto.bank;

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
@XmlRootElement(name = "AccountRecord")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountRecord {
    @XmlElement(name = "BankCode")
    private String bankCode;

    @XmlElement(name = "AccountNumber")
    private String accountNumber;

}
