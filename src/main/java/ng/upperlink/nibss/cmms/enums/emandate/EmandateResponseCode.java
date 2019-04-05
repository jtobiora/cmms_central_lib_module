package ng.upperlink.nibss.cmms.enums.emandate;

public enum EmandateResponseCode {
    CREATION_SUCCESSFUL("00","Mandate creation was successful and approved"),
    ENTER_OTP("00","Please enter OTP sent to your phone"),
    PAYMENT_SUCCESSFUL("00","Payment successful"),
    ITEM_FOUND("00","Item found"),
    INVALID_REQUEST("06","some of the inputs are invalid"),
    INVALID_ACCOUNT_INFO("07","Invalid or wrong account details provided"),
    INVALID_SUBSCRIBER_INFO("08","Invalid subscriber information provided"),
    INVALID_START_DATE("09", "Mandate start date cannot be today {} or less!"),
    INVALID_FREQUENCY("10", "Specify a valid frequency for fixed mandate"),
    INVALID_DATE_RANGE("11","Mandate date range must be able to accommodate frequency!"),
    PRODUCT_NOT_FOUND("12","Could not find the product with id: {}"),
    PRODUCT_BILLER_NOT_FOUND("13","Biller attached to product with id: {} is not found"),
    SUBSCRIBER_BANK_NOT_FOUND("14","Subscriber's bank with code: {} has not been enrolled on CMMS"),
//    EMPTY_MANDATE_REQUEST("15","Mandate request is not empty, try again"),
    BILLER_NOT_FOUND("15","Biller authentication failed,provide a valid credentials"),
    BANK_NOT_FOUND("16","Bank authentication failed,provide a valid credentials"),
//    CLIENT_NOT_FOUND("18","Client authentication failed,provide a valid credentials"),
    MANDATE_NOT_FOUND("17","Mandate with the reference code: {} is not found"),
    CHANNEL_NOT_FOUND("18","Channel with code: {} does not exist"),
    INVALID_SESSION_ID("19","The sessionId {} provided is invalid"),
    INVALID_CREDENTIALS("20","Invalid e-mandate credentials"),


    NOT_ACCEPTABLE("21","Bank approval failed, try again"),
    UNAUTHORIZED("22","Unauthorized access only {} is authorized"),
    EMANDAT_DISABLED("23","Electronic mandate is not enabled on {}. Please contact admin for reactivation"),
    PAYMENT_NOT_ALLOWED("24"," Instant debit is not authorized on fixed mandate."),
    PARSE_DATE_ERROR("25","Invalid date format {}: must be yyyy-MM-dd"),
    MANDATE_NOT_GENERATEED("26","Mandate setup failed, please try again"),
    INVALID_ACCOUNT_NUMBER("27","Invalid account number"),
    ACCOUNT_NUMBER_EXIST("28","Account number {} already exist"),
    UNTHORIZED_AMOUNT("29","Cannot debit more than initial configured amount {} "),
    UNTHORIZED_MANDATE("30","Subscriber's bank has not authorize debit "),
    INVALID_MRC("31","Invalid MRC : {}"),
    MRC_DEACTIVATED("32","MRC: {} has been deactivated"),
    INVALID_MRC_PIN("33","Invalid MRC pin "),
    INVALID_RECOVERY_CODE("34","Invalid recovery code"),
    RECOVERY_CODE_USED("35","Recovery code has been used"),
    RECOVERY_CODE_EXPIRED("36","Recovery code has expired."),
    UNAUTHORIZED_MRC_UPDATE("37","Unauthorized MRC update"),
    TRANSACTION_NOT_FOUND("38","Transaction has not been initiated"),
    OTP_VALIDATION_FAILED("39","OTP validation failed"),
    INVALID_PHONE_NUMBER("50","User mobile number is invalid"),
    NOT_USER_OTP("51","Token not associated with the specified user"),
    INVALID_BANKCODE("52","Invalid Bank Code"),
    SMS_NOT_SENT("53","Could not send SMS"),
    INVALID_OTP("54","OTP {} provided is invalid"),
    PAYMENT_FAILED("55","Payment failed"),
    PAYMENT_ENTRED("56","Payment Entered"),
    PAYMENT_REVERSED("57","Payment Reversed"),
    PAYMENT_IN_PROGRESS("58","Payment in progress"),
    EXPIRED_OTP("59","User token has expired"),
//    NULL_POINTER("101","Null: invalid input. Please try again"),


    //    EMPTY_CREDENTIAL("27","Username or password or Api key is empty"),
//    INVALID_API_KEY("28","Invalid Api key"),
    MANDATE_STATUS_REQUEST_FAILED("29","could not retrieve mandate status"),
    MCASH_SESSION_ID_GENERATION_FAILED("30","Could not generate the mcash sessionId"),
    MCASH_REQUEST_NOT_GENERATED("31","Could not generate mcash request"),
    MCASH_XML_REQUEST_NOT_GENERATED("32","Could not generate mcash request"),
    MCASH_AUTHENTICATION_FAILED("33","Error occurred during bank authorization"),
    MCASH_NO_RESPONSE("34","Could not get response from subscriber's bank {} "),


    UNKNOWN("100","Unexpected error occurred, Please try again!");



    String code;
    String value;

    EmandateResponseCode(String code, String value) {
        this.code = code;
        this.value = value;
    }
    private static EmandateResponseCode findByValue(String value) {
        EmandateResponseCode type = null;

        for (EmandateResponseCode roleName : EmandateResponseCode.values()) {
            if( roleName.value.equalsIgnoreCase(value)) {
                type = roleName;
                break;
            }
        }
        return  type == null ? UNKNOWN : type;

    }
private EmandateResponseCode findByCode(String code) {
        EmandateResponseCode type = null;

        for (EmandateResponseCode emandateResponseCode : EmandateResponseCode.values()) {
            if( emandateResponseCode.code.equals(code)) {
                type = emandateResponseCode;
                break;
            }
        }
        return  type == null ? UNKNOWN : type;

    }
    public String getValue() {
        return  value;
    }
    public String getCode(){return code;}
}
