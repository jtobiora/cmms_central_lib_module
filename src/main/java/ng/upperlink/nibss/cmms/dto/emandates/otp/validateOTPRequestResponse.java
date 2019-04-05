package ng.upperlink.nibss.cmms.dto.emandates.otp;

import lombok.NoArgsConstructor;
import ng.upperlink.nibss.cmms.util.emandate.soap.bank.XMLBuilder;

@NoArgsConstructor
public class validateOTPRequestResponse {
    private String response;

    public ValidateOTPResponse getResponse() {
        return XMLBuilder.unmarshalWithoutDecrypting(response, ValidateOTPResponse.class);
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
