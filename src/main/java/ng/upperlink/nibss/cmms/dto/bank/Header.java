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
@XmlRootElement(name = "Header")
@XmlAccessorType(XmlAccessType.FIELD)
public class Header {
    @XmlElement(name = "ClientId")
    private String clientId;//const

    @XmlElement(name = "EnquiryId")
    private String enquiryId;//generated

    @XmlElement(name = "Salt")
    private String salt;//generated

    @XmlElement(name = "Mac")
    private String mac;//hashed generated clientId+"-"+secreteKey+"-"+salt
}
