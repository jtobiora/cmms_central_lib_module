package ng.upperlink.nibss.cmms.dto.emandates.otp;

import lombok.NoArgsConstructor;
import ng.upperlink.nibss.cmms.util.emandate.soap.bank.XMLBuilder;


@NoArgsConstructor
public class generateOTPRequestResponse {
    String response;


    public void setResponse(String response) {
        this.response = response;
    }
    public GenerateOTPResponse generateOTPResponse(){
     return XMLBuilder.unmarshalWithoutDecrypting(response,GenerateOTPResponse.class);
    }

}
