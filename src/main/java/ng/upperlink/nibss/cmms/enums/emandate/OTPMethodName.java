package ng.upperlink.nibss.cmms.enums.emandate;

public enum OTPMethodName {
    GENERATE_OTP("generateOTPRequest"),
    VALIDATE_OTP("validateOTPRequest");
     private String value;
    OTPMethodName(String value) {
        this.value =value;
    }
    public String getValue() {
        return  value;
    }
}
