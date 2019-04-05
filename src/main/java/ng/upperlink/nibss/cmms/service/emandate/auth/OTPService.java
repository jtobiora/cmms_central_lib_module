package ng.upperlink.nibss.cmms.service.emandate.auth;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.emandates.AuthParam;
import ng.upperlink.nibss.cmms.dto.emandates.otp.*;
import ng.upperlink.nibss.cmms.dto.emandates.response.EmandateResponse;
import ng.upperlink.nibss.cmms.enums.UserType;
import ng.upperlink.nibss.cmms.enums.emandate.EmandateResponseCode;
import ng.upperlink.nibss.cmms.enums.emandate.OTPMethodName;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.emandate.Subscriber;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.service.account.ICADService;
import ng.upperlink.nibss.cmms.service.emandate.BankEmandateService;
import ng.upperlink.nibss.cmms.service.emandate.BillerEmandateService;
import ng.upperlink.nibss.cmms.service.emandate.EmandateBaseService;
import ng.upperlink.nibss.cmms.service.emandate.SubscriberService;
import ng.upperlink.nibss.cmms.util.emandate.soap.bank.XMLBuilder;
import ng.upperlink.nibss.cmms.util.emandate.soap.builder.SoapBuilder;
import ng.upperlink.nibss.cmms.util.emandate.soap.builder.SoapPayload;
import ng.upperlink.nibss.cmms.util.emandate.soap.enums.SoapTransformerMode;
import ng.upperlink.nibss.cmms.util.emandate.soap.transformer.soap.AccountValidationTransformer;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class OTPService {

    private static final String billerId = "NIBSS0000000030";
    private static final String billerName ="Upperlink";
    private static final String billerTransOd = "1045620";
    private static final String KEY = "67651F8E63889980F83AD46C3DB0A27B";
    private static final String ENDPOINT = "https://staging.nibss-plc.com.ng/CentralPayWebservice/CentralPayOperations";

    public static String getKEY() {
        return KEY;
    }

    private RedisService redisServiceMandate;
    @Autowired
    SubscriberService subscriberService;
    @Autowired
    public void setRedisService(RedisService redisService) {
        this.redisServiceMandate = redisService;
    }

    @Autowired
    private EmandateBaseService emandateBaseService;
    @Autowired
    private BillerEmandateService billerEmandateService;

    @Autowired
    private BankEmandateService bankEmandateService;
    public static Object setupOTPRequest(String mandateCode, String bankCode, String amount, String otp, OTPMethodName methodName ) throws CMMSException {
        SoapBuilder xmlRequest;
        String xml;
        switch (methodName)
        {
            case GENERATE_OTP:
                GenerateOTPRequest request = generateOTPRequestObj(mandateCode, bankCode, amount);
                xmlRequest = XMLBuilder.generateOTPRequestXML(request);
//                xml = XMLBuilder.marshal(request, GenerateOTPRequest.class);
//                System.out.println("Mashaller: \n"+xml);
                break;
            case VALIDATE_OTP:
                ValidateOTPRequest validateOTPRequest = generateValidateOTPRequestObj(mandateCode, bankCode, amount, otp);
                xmlRequest = XMLBuilder.generateValidateOTPRequestXML(validateOTPRequest);
//                xml = XMLBuilder.marshal(validateOTPRequest,ValidateOTPRequest.class);
                break;
            default:
                throw new CMMSException("method called not allowed","401", EmandateResponseCode.UNAUTHORIZED.getCode());
        }
//        System.out.println("With hash\n"+xml);
        return callOPT(xmlRequest,methodName.getValue());

    }

    private static Object callOPT(SoapBuilder xmlRequest, String methodName) throws CMMSException {
        try {
            SoapBuilder soapRequest = buildSoapRequest(xmlRequest, methodName);

            OTPWebService otpWebService = new OTPWebService(ENDPOINT, "otp", soapRequest.getXml());
            Object response = otpWebService.getResponse().getBody();

            String responseString = response != null ? response.toString() : "";

            responseString = responseString.replace("return", "response");
          /*  responseString = StringEscapeUtils.unescapeXml(responseString);
            log.info("Response <<<<<<>>>>>>> "+responseString);*/

            if (methodName.equalsIgnoreCase(OTPMethodName.GENERATE_OTP.getValue()))
            {
                // Casting the XML string to a Class
                AccountValidationTransformer<generateOTPRequestResponse> accountValidationTransformer = new AccountValidationTransformer<>();
                generateOTPRequestResponse otpResponse = accountValidationTransformer.transformBody(responseString, generateOTPRequestResponse.class, SoapTransformerMode.ROOT);
                return otpResponse.generateOTPResponse();
            }
            if (methodName.equalsIgnoreCase(OTPMethodName.VALIDATE_OTP.getValue())) {// Casting the XML string to a Class
                AccountValidationTransformer<validateOTPRequestResponse> accountValidationTransformer = new AccountValidationTransformer<>();
                validateOTPRequestResponse otpResponse = accountValidationTransformer.transformBody(responseString, validateOTPRequestResponse.class, SoapTransformerMode.ROOT);
                return otpResponse.getResponse();

            }
//            System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"+accountDetailsResponse.getAccountDetailsResult());

            throw new CMMSException(EmandateResponseCode.UNKNOWN.getValue(),"500",EmandateResponseCode.UNKNOWN.getCode());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("OTP generation failed {}",e);
            throw new CMMSException(EmandateResponseCode.UNKNOWN.getValue(),"500",EmandateResponseCode.UNKNOWN.getCode());
        }
    }

    private static SoapBuilder buildSoapRequest(SoapBuilder inputXMLrequest, String methodName) throws Exception {

//        System.out.println(">>>>>>>>>>>>>>\n "+inputXMLrequest);

        Map<String, String> namespace = new HashMap<String, String>() {{
            put("web", "http://web.nibss.com/");
//                put("xsd", "http://www.w3.org/2001/XMLSchema");
//                put("urn", "urn:css_name_enquiry_webservice");
        }};


        SoapBuilder soapBuilder = new SoapBuilder("soapenv", namespace);
        soapBuilder.set("soapenv__Header", new SoapPayload());
        soapBuilder.set("soapenv__Body", new SoapPayload());
        soapBuilder.get("soapenv__Body").set("web__"+methodName, new SoapPayload());
        soapBuilder.get("soapenv__Body").get("web__"+methodName).set("arg0", inputXMLrequest.getAsPayload());

        return soapBuilder;
    }

    public static String randomString(char[] characterSet, int length) {
        Random random = new SecureRandom();
        char[] result = new char[length];
        for (int i = 0; i < result.length; i++) {
            // picks a random index out of character set > random character
            int randomCharIndex = random.nextInt(characterSet.length);
            result[i] = characterSet[randomCharIndex];
        }
        return new String(result);
    }

    private static GenerateOTPRequest generateOTPRequestObj(String mandateCode, String bankCode, String amount)
    {
        GenerateOTPRequest request = new GenerateOTPRequest();
        request.setMandateCode(mandateCode);
        request.setBankCode(bankCode);
        request.setAmount(amount);
        request.setBillerId(billerId);
        request.setBillerName(billerName);
        request.setBillerTransOd(billerTransOd);

        /** Do sha 256 and set the value returned to hashValue field*/
//        String sha256 = doSha256(request,GenerateOTPRequest.class);
//        request.setHashValue(sha256);
        return request;
    }

    private static <T> String doSha256(T request,Class<T> clazz) {
        String xml = XMLBuilder.marshal(request, clazz)+KEY;
        System.out.println("Without hash\n"+xml);
        return EncyptionUtil.doSHA512Encryption(xml);
    }

    private static ValidateOTPRequest generateValidateOTPRequestObj(String mandateCode, String bankCode, String amount,String otp)
    {
        ValidateOTPRequest request = new ValidateOTPRequest();
        request.setMandateCode(mandateCode);
        request.setBankCode(bankCode);
        request.setOtp(otp);
        request.setBillerId(billerId);
        request.setBillerName(billerName);
        request.setAmount(amount);
        request.setBillerTransOd(billerTransOd);

        /** Do sha 256 and set the value returned to hashValue field*/
        /*String sha256 = doSha256(request,ValidateOTPRequest.class);
        request.setHashValue(sha256);*/
        return request;
    }

//    private static String doSha256validateOTP(ValidateOTPRequest request) {
//        String xml = XMLBuilder.marshal(request, ValidateOTPRequest.class)+KEY;
//        System.out.println(xml);
//        return EncyptionUtil.doSHA512Encryption(xml);
//    }


    public ResponseEntity<?> validateOTP(String sessiondId, ValidateOTP validateOTP,UserType userType) {
        OTPResponse otpResponse = null;
        AuthParam authParam = validateOTP.getAuthParam();
        EmandateResponse emandateResponse =null;

        if (StringUtils.isEmpty(validateOTP.getSessionId())) {
            otpResponse = generateOTPResponse(EmandateResponseCode.INVALID_SESSION_ID, sessiondId, validateOTP.getSessionId());
            return ResponseEntity.ok(otpResponse);
        }
        if (StringUtils.isEmpty(validateOTP.getOtp())) {
            otpResponse = generateOTPResponse(EmandateResponseCode.INVALID_OTP, sessiondId, validateOTP.getOtp());
            return ResponseEntity.ok(otpResponse);
        }
        try {
            if (userType.equals(UserType.BANK))
                emandateBaseService.authenticateBank(authParam);
            else
                emandateBaseService.authenticateBiller(authParam);
            String tokenFromRedis = redisServiceMandate.findTokenFromRedis(sessiondId);
            if (tokenFromRedis ==null)
            {
                return ResponseEntity.ok(generateOTPResponse(EmandateResponseCode.INVALID_SESSION_ID,null,""));
            }
            log.info("JWT object retrieved is \n"+tokenFromRedis);
            Mandate mandate = redisServiceMandate.decodeToken(tokenFromRedis, true, Mandate.class);
            if (mandate ==null)
            {
                otpResponse = generateOTPResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,sessiondId,"");
                return ResponseEntity.ok(otpResponse);
            }
            ValidateOTPResponse validateOTPResponse = (ValidateOTPResponse) setupOTPRequest(mandate.getMandateCode(), mandate.getBank().getCode(), String.valueOf(mandate.getAmount()), validateOTP.getOtp(), OTPMethodName.VALIDATE_OTP);
            if (StringUtils.isEmpty(validateOTPResponse.getResponseCode()))
            {
                otpResponse =generateOTPResponse(EmandateResponseCode.OTP_VALIDATION_FAILED,sessiondId,"");
                return ResponseEntity.ok(otpResponse);
            }

            else if (!validateOTPResponse.getResponseCode().equalsIgnoreCase("00"))
            {
                otpResponse =generateOTPResponse(EmandateResponseCode.OTP_VALIDATION_FAILED,sessiondId,"");
                return getOTPResponseEntity(sessiondId,otpResponse.getResponseCode());
            }else {
                if (userType.equals(UserType.BANK))
                    emandateResponse = bankEmandateService.saveMandate(mandate);
                else
                    emandateResponse = billerEmandateService.saveOperation(mandate);
                return ResponseEntity.ok(emandateResponse);
            }
        }catch (CMMSException e)
        {
            log.error("CMMSException {}",e);
            return ResponseEntity.ok(new EmandateResponse(e.getEmandateErrorCode(),null,e.getMessage()));
        }
    }



    public ResponseEntity<?> validateOTPGeneric(String sessiondId, ValidateOTP validateOTP) {
        OTPResponse otpResponse = null;
        AuthParam authParam = validateOTP.getAuthParam();
        EmandateResponse emandateResponse =null;

        if (StringUtils.isEmpty(validateOTP.getSessionId())) {
            otpResponse = generateOTPResponse(EmandateResponseCode.INVALID_SESSION_ID, sessiondId, validateOTP.getSessionId());
            return ResponseEntity.ok(otpResponse);
        }
        if (StringUtils.isEmpty(validateOTP.getOtp())) {
            otpResponse = generateOTPResponse(EmandateResponseCode.INVALID_OTP, sessiondId, validateOTP.getOtp());
            return ResponseEntity.ok(otpResponse);
        }
        try {
            emandateBaseService.authenticateBiller(authParam);
            String tokenFromRedis = redisServiceMandate.findTokenFromRedis(sessiondId);
            if (tokenFromRedis ==null)
            {
                return ResponseEntity.ok(generateOTPResponse(EmandateResponseCode.INVALID_SESSION_ID,null,""));
            }
            Subscriber subscriber = redisServiceMandate.decodeToken(tokenFromRedis, false, Subscriber.class);
            if (subscriber ==null)
            {
                otpResponse = generateOTPResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,sessiondId,"");
                return ResponseEntity.ok(otpResponse);
            }
            ValidateOTPResponse validateOTPResponse = (ValidateOTPResponse) setupOTPRequest(subscriber.getMrc(), subscriber.getBank().getCode(), "1", validateOTP.getOtp(), OTPMethodName.VALIDATE_OTP);
            if (StringUtils.isEmpty(validateOTPResponse.getResponseCode()))
            {
                otpResponse =generateOTPResponse(EmandateResponseCode.OTP_VALIDATION_FAILED,sessiondId,"");
                return ResponseEntity.ok(otpResponse);
            }

            if (!validateOTPResponse.getResponseCode().equalsIgnoreCase("00"))
            {
                otpResponse =generateOTPResponse(EmandateResponseCode.OTP_VALIDATION_FAILED,sessiondId,"");
                return getOTPResponseEntity(sessiondId,otpResponse.getResponseCode());

            }else
                return subscriberService.performSaveOperation(subscriber);
        }catch (CMMSException e)
        {
            log.error("CMMSException {}",e);
            return ResponseEntity.ok(new EmandateResponse(e.getEmandateErrorCode(),null,e.getMessage()));
        }
    }


    public static OTPResponse generateOTPResponse(EmandateResponseCode emandateResponseCode, String sessionID, String replace) {
        OTPResponse otpResponse;
        otpResponse = new OTPResponse(emandateResponseCode.getCode(),sessionID,emandateResponseCode.getValue().replace("{}",replace));
        log.info(otpResponse.toString());
        return otpResponse;
    }

    public static ResponseEntity getOTPResponseEntity(String sessionId, String responseCode) {
        OTPResponse otpResponse;
        if (responseCode.equalsIgnoreCase("00"))
        {
            otpResponse = generateOTPResponse(EmandateResponseCode.ENTER_OTP,sessionId,"");
            return ResponseEntity.ok(otpResponse);
        }else
            return ResponseEntity.ok(setOTPResponseCode(responseCode,sessionId));
    }
    public static OTPResponse setOTPResponseCode(String responseCode,String sessionId)
    {
        switch (responseCode)
        {
            case "02":
                return generateOTPResponse(EmandateResponseCode.INVALID_ACCOUNT_NUMBER,sessionId,"");
            case "03":
                return generateOTPResponse(EmandateResponseCode.EXPIRED_OTP,sessionId,"");
            case "06":
                return generateOTPResponse(EmandateResponseCode.NOT_USER_OTP,sessionId,"");
            case "08":
                return generateOTPResponse(EmandateResponseCode.INVALID_BANKCODE,sessionId,"");
            case "13":
                return generateOTPResponse(EmandateResponseCode.SMS_NOT_SENT,sessionId,"");
            default:
                return generateOTPResponse(EmandateResponseCode.UNKNOWN,sessionId,"");
        }
    }
    public static void main(String[] args) {
        try {
            ICADService.createSecuredManager();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
       /* try {
            GenerateOTPResponse getValidateOTPResponse = (GenerateOTPResponse) setupOTPRequest("rlfkl", "400", "22", "3049", OTPMethodName.GENERATE_OTP);
            System.out.println(getValidateOTPResponse);
        } catch (CMMSException e) {
            e.printStackTrace();
            log.error("Ex ",e);
        }*/
       try {
            ValidateOTPResponse getValidateOTPResponse = (ValidateOTPResponse) setupOTPRequest("rlfkl", "400", "22", "3049", OTPMethodName.VALIDATE_OTP);
            System.out.println("Validate response\n"+getValidateOTPResponse);
        } catch (CMMSException e) {
            e.printStackTrace();
            log.error("Ex ",e);
        }

       /* GenerateOTPRequest request = generateOTPRequestObj("sos", "032", "20");
        String xml = XMLBuilder.marshal(request, GenerateOTPRequest.class);
        System.out.println(xml);
        ValidateOTPRequest res = generateValidateOTPRequestObj("love","32","100","23456");
        xml = XMLBuilder.marshal(res,ValidateOTPRequest.class);
        System.out.println("This is the VOTP\n"+xml);*/

       /*try {

           generateOTPRequestResponse generateOTPResponse = setupOTPRequest("99989634673943048016", "070", "500", null, OTPMethodName.GENERATE_OTP, generateOTPRequestResponse.class);
           System.out.println(generateOTPResponse);
       }catch (CMMSException e)
       {
           log.error("CMMSException caught ",e);
       } catch (NoSuchAlgorithmException e) {
           e.printStackTrace();
       } catch (KeyManagementException e) {
           e.printStackTrace();
       }*/
    }

}
