package ng.upperlink.nibss.cmms.service.emandate;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.emandates.*;
import ng.upperlink.nibss.cmms.dto.emandates.otp.*;
import ng.upperlink.nibss.cmms.dto.emandates.response.EmandateResponse;
import ng.upperlink.nibss.cmms.dto.emandates.response.StatusResponse;
import ng.upperlink.nibss.cmms.enums.*;
import ng.upperlink.nibss.cmms.enums.emandate.EmandateResponseCode;
import ng.upperlink.nibss.cmms.enums.emandate.McashResponseCode;
import ng.upperlink.nibss.cmms.enums.emandate.OTPMethodName;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.bank.Bank;
import ng.upperlink.nibss.cmms.model.biller.Biller;
import ng.upperlink.nibss.cmms.model.biller.Product;
import ng.upperlink.nibss.cmms.model.emandate.Subscriber;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.model.mandate.MandateStatus;
import ng.upperlink.nibss.cmms.repo.MandateRepo;
import ng.upperlink.nibss.cmms.service.bank.BankService;
import ng.upperlink.nibss.cmms.service.biller.BillerService;
import ng.upperlink.nibss.cmms.service.emandate.auth.OTPService;
import ng.upperlink.nibss.cmms.service.emandate.auth.RedisService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateStatusService;
import ng.upperlink.nibss.cmms.util.DateUtils;
import ng.upperlink.nibss.cmms.util.emandate.EMandateValidator;
import ng.upperlink.nibss.cmms.util.emandate.JsonBuilder;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
public class EmandateBaseService {
    @Value("${encryption.salt}")
    private String salt;
//    private static final Logger lo = LoggerFactory.getLogger(EmandateBaseService.class);
    private BankService bankService;
    private BillerService billerService;
    private EMandateValidator eMandateValidator;
    private BankEmandateService bankEmandateService;
    private BillerEmandateService billerEmandateService;
    private MandateRepo mandateRepo;
    private MandateStatusService mandateStatusService;
    private SubscriberService subscriberService;
    private RedisService redisService;

    @Autowired
    public void setRedisService(RedisService redisService) {
        this.redisService = redisService;
    }
    @Autowired
    public void setSubscriberService(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }
    @Autowired
    public void setMandateStatusService(MandateStatusService mandateStatusService){
        this.mandateStatusService = mandateStatusService;
    }
    @Autowired
    public void setBankService(BankService bankService)
    {
        this.bankService = bankService;
    }
    @Autowired
    public void setBillerService(BillerService billerService)
    {
        this.billerService = billerService;
    }
    @Autowired
    public void seteMandateValidator(EMandateValidator eMandateValidator) {
        this.eMandateValidator = eMandateValidator;
    }
    @Autowired
    public void setBankEmandateService(BankEmandateService bankEmandateService) {
        this.bankEmandateService = bankEmandateService;
    }
    @Autowired
    public void setBillerEmandateService(BillerEmandateService billerEmandateService) {
        this.billerEmandateService = billerEmandateService;
    }
    @Autowired
    public void setMandateRepo(MandateRepo mandateRepo) {
        this.mandateRepo = mandateRepo;
    }

    public Bank authenticateBank(AuthParam auth) throws CMMSException {
            if (StringUtils.isEmpty(auth.getUsername()))
            {
                log.info("Username cannot be null");
                throw new CMMSException("pass code cannot be null","400",EmandateResponseCode.INVALID_CREDENTIALS.getCode());

            }
                Bank bank = bankService.getBankByUsername(auth.getUsername());
            if (bank ==null)
            {
                log.error("Client is not found : Invalid credentials ");
                throw new CMMSException("Client is not found : Invalid credentials ","400",EmandateResponseCode.BANK_NOT_FOUND.getCode());
            }
            if (StringUtils.isEmpty(auth.getPassword()))
            {
                log.info("pass code cannot be null");
                throw new CMMSException("pass code cannot be null","400",EmandateResponseCode.INVALID_CREDENTIALS.getCode());
            }
            if (!EncyptionUtil.doSHA512Encryption(auth.getPassword(),salt).equals(bank.getEmandateConfig().getPassword()))
            {
                log.info("Bank Password does not match");
                throw new CMMSException("Bank Password does not match","400",EmandateResponseCode.INVALID_CREDENTIALS.getCode());
            }
            if (!bank.getApiKey().equals(auth.getApiKey()))
            {
                log.info("Bank Api does not match");
                throw new CMMSException("Bank Api does not match","400",EmandateResponseCode.INVALID_CREDENTIALS.getCode());
            }
        if (!bank.getEmandateConfig().isActivated())
        {
            log.error("E mandate is disabled");
            throw new CMMSException(EmandateResponseCode.EMANDAT_DISABLED.getValue().replace("{}",Optional.ofNullable(bank.getName()).orElse("this Bank")),"401",EmandateResponseCode.EMANDAT_DISABLED.getCode());
        }
        log.info("Requeting Bank\n Name: "+Optional.ofNullable(bank.getName())+"\n"+"Username: "+Optional.ofNullable(bank.getEmandateConfig().getUsername()));


        return bank;
    }
    public Biller authenticateBiller(AuthParam auth) throws CMMSException {if (StringUtils.isEmpty(auth.getUsername()))
    {
        log.info("Username cannot be null");
        throw new CMMSException("pass code cannot be null","400",EmandateResponseCode.INVALID_CREDENTIALS.getCode());
    }
        Biller biller = billerService.getBillerByUsername(auth.getUsername());
        if (biller ==null)
        {
            log.error("Biller is not found : Invalid credentials ");
            throw new CMMSException("Biller is not found : Invalid credentials ","400",EmandateResponseCode.BILLER_NOT_FOUND.getCode());
        }
        if (StringUtils.isEmpty(auth.getPassword()))
        {
            log.info("pass code cannot be null");
            throw new CMMSException("pass code cannot be null","400",EmandateResponseCode.INVALID_CREDENTIALS.getCode());
        }
        if (!EncyptionUtil.doSHA512Encryption(auth.getPassword(),salt).equals(biller.getEmandateConfig().getPassword()))
        {
            log.info("Biller Password does not match");
            throw new CMMSException("Biller Password does not match","400",EmandateResponseCode.INVALID_CREDENTIALS.getCode());
        }
        if (!biller.getApiKey().equals(auth.getApiKey()))
        {
            log.info("Biller Api does not match");
            throw new CMMSException("Biller Api does not match","400",EmandateResponseCode.INVALID_CREDENTIALS.getCode());
        }

        if (!biller.getEmandateConfig().isActivated())
        {
            log.error("E mandate is disabled");
            throw new CMMSException(EmandateResponseCode.EMANDAT_DISABLED.getValue().replace("{}",Optional.ofNullable(biller.getName()).orElse("this Biller")),"401",EmandateResponseCode.EMANDAT_DISABLED.getCode());
        }
        log.info("Requeting Biller\n Name: "+Optional.ofNullable(biller.getName())+"\n"+"Username: "+Optional.ofNullable(biller.getEmandateConfig().getUsername()));
        return biller;
    }
    public ResponseEntity<?> processIncomingRequest(EMandateRequestBody eMandateRequestBody, UserType userType,String sessionId){
        sessionId = sessionId+"-"+String.valueOf((new Date()).getTime());
        EmandateResponse emandateResponse = null;
        EmandateRequest emandateRequest = eMandateRequestBody.getEmandateRequest();
        AuthParam authParam =eMandateRequestBody.getAuth();
        String jSonResponse;
        try {
            if (emandateRequest ==null)
            {
                emandateResponse = generateEmandateResponse(EmandateResponseCode.INVALID_REQUEST,null,"");
                return ResponseEntity.ok(emandateResponse);
            }
            switch (userType)
            {
                case BANK:
                    emandateResponse = eMandateValidator.validate(emandateRequest);
                    if (emandateResponse != null)
                    {
                        jSonResponse = JsonBuilder.generateJson(emandateResponse);
                        log.info(jSonResponse);
                        return ResponseEntity.ok(emandateResponse);
                    }
                   authenticateBank(authParam);

                    emandateResponse = bankEmandateService.processMandateWithoutMRC(emandateRequest);
                    return returnEmandateResponse(emandateResponse);
                case BILLER:
                    emandateResponse = eMandateValidator.validate(emandateRequest);
                    if (emandateResponse != null)
                    {
                        log.info(emandateResponse.toString());
                        return ResponseEntity.ok(emandateResponse);
                    }
                    authenticateBiller(authParam);
                    return billerEmandateService.processMandate(emandateRequest, sessionId);

            }
            return ResponseEntity.ok(emandateResponse);
        } catch (JsonProcessingException e) {
            log.error("Json Error {}",e);
            e.printStackTrace();
           return returnEmandateResponse( new EmandateResponse(EmandateResponseCode.UNKNOWN.getCode(),null,EmandateResponseCode.UNKNOWN.getValue()));
        } catch (CMMSException e) {
            e.printStackTrace();
            return returnEmandateResponse(new EmandateResponse(e.getEmandateErrorCode(),null,e.getMessage()));
        }

    }

    public ResponseEntity<?> processMRCRequest(MRCMandateBody mrcMandateBody, UserType userType,String sessionId){
        sessionId = sessionId+"-"+String.valueOf((new Date()).getTime());
        EmandateResponse emandateResponse = null;
        MRCMandateRequest mrcMandateRequest = mrcMandateBody.getMrcMandateRequest();
        String jSonResponse;
        try {
            if (mrcMandateRequest ==null)
            {
                emandateResponse = generateEmandateResponse(EmandateResponseCode.INVALID_REQUEST,null,"");
                return ResponseEntity.ok(emandateResponse);
            }
            switch (userType)
            {
                case BANK:
                    emandateResponse = eMandateValidator.validate(mrcMandateRequest);
                    if (emandateResponse != null)
                    {
                        jSonResponse = JsonBuilder.generateJson(emandateResponse);
                        log.info(jSonResponse);
                        return ResponseEntity.ok(emandateResponse);
                    }
                   authenticateBank(mrcMandateBody.getAuthParam());

                    return bankEmandateService.processMandateWithMRC(mrcMandateRequest,sessionId);
                case BILLER:
                    emandateResponse = eMandateValidator.validate(mrcMandateRequest);
                    if (emandateResponse != null)
                    {
                        log.info(emandateResponse.toString());
                        return ResponseEntity.ok(emandateResponse);
                    }
                    authenticateBiller(mrcMandateBody.getAuthParam());
                    return billerEmandateService.processMandateWithMRC(mrcMandateRequest, sessionId);

            }
            return ResponseEntity.ok(emandateResponse);
        } catch (JsonProcessingException e) {
            log.error("Json Error {}",e);
            e.printStackTrace();
           return returnEmandateResponse( new EmandateResponse(EmandateResponseCode.UNKNOWN.getCode(),null,EmandateResponseCode.UNKNOWN.getValue()));
        } catch (CMMSException e) {
            e.printStackTrace();
            return returnEmandateResponse(new EmandateResponse(e.getEmandateErrorCode(),null,e.getMessage()));
        }

    }

    private ResponseEntity<?> returnEmandateResponse(EmandateResponse emandateResponse) {
        if (!emandateResponse.getResponseCode().equals(EmandateResponseCode.CREATION_SUCCESSFUL.getCode()))
        {
            log.info(emandateResponse.toString());
            return ResponseEntity.ok(emandateResponse);
        }else
        {
            log.info(emandateResponse.toString());
            return ResponseEntity.ok(emandateResponse);
        }
    }

    public static EmandateResponse generateEmandateResponse(EmandateResponseCode emandateResponseCode,String mandateCode,String replace) {
        EmandateResponse emandateResponse;
        emandateResponse = new EmandateResponse(emandateResponseCode.getCode(),mandateCode,emandateResponseCode.getValue().replace("{}",replace));
        log.info(emandateResponse.toString());
        return emandateResponse;
    }
    public static EmandateResponse generateMcashResponse(McashResponseCode mcashResponseCode, String mandateCode)  {
        EmandateResponse emandateResponse;
        emandateResponse = new EmandateResponse(Optional.ofNullable(mcashResponseCode.getCode()).orElse(EmandateResponseCode.UNKNOWN.getCode()),mandateCode,Optional.ofNullable(mcashResponseCode.getValue()).orElse(EmandateResponseCode.UNKNOWN.getValue()));
        log.info(emandateResponse.toString());
        return emandateResponse;
    }

    public EmandateResponse setEmandateResponse(ResponseEntity responseEntity)
    {
        Mandate mandate =null;
        String errorMsg = null;
        if (responseEntity.getBody() instanceof Mandate) {
            mandate = (Mandate) responseEntity.getBody();
        }
        if (responseEntity.getBody() instanceof String) {
            errorMsg = (String) responseEntity.getBody();
        }

        return new EmandateResponse(responseEntity.getStatusCode().toString(),mandate.getMandateCode(),errorMsg);
    }

    public Mandate findByMandateCode(String mandateCode)
    {
        try {
            return mandateRepo.findByMandateCode(mandateCode);
        }catch (Exception e)
        {
            log.error("--Exception occured-- ",e);
            return null;
        }
    }

    public  ResponseEntity<?> getMandateStatus(StatusRequest statusRequest, UserType userType){
        StatusResponse statusResponse = null;
        Mandate mandate=null;
        try {
            switch (userType) {
                case BILLER:
                    authenticateBiller(statusRequest.getAuth());
                    break;
                case BANK:
                    authenticateBank(statusRequest.getAuth());
                    break;
            }
            mandate = findByMandateCode(statusRequest.getMandateCode());
            if (mandate == null) {
                statusResponse = new StatusResponse(statusRequest.getMandateCode(), EmandateResponseCode.MANDATE_NOT_FOUND.getValue().replace("{}",statusRequest.getMandateCode()), EmandateResponseCode.MANDATE_NOT_FOUND.getCode());
                return ResponseEntity.ok(statusResponse);
            }
            statusResponse = new StatusResponse(mandate.getMandateCode(), mandate.getStatus().getName(), EmandateResponseCode.ITEM_FOUND.getCode());
            return ResponseEntity.ok(statusResponse);
        }catch (CMMSException e)
        {
            e.printStackTrace();
            statusResponse = new StatusResponse(statusRequest.getMandateCode(), null, e.getEmandateErrorCode());
            return ResponseEntity.ok(statusResponse);
        }
    }

    public StatusResponse buildStatusResponse(EmandateResponseCode responseCode,StatusRequest request,Mandate mandate)
    {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String mandateCode = mandate.getMandateCode();
        String status =  mandate.getStatus().getDescription();
        String statusCode =  mandate.getStatus().getCode();
        String startDate = f.format(mandate.getStartDate());
        String endDate = f.format(mandate.getEndDate());
        String nextDebitDate =f.format(mandate.getNextDebitDate());
        String debitAuthorized;
        switch (mandate.getMandateAdviceResponseCode())
        {
            case "00": debitAuthorized = "";
            case "":debitAuthorized ="";
        }
        return null;
    }
    public Mandate generateMandate(Mandate mandate, EmandateRequest emandateRequest,
                                   String mandateCode, Product product, Bank bank, Biller biller,Channel channel) throws CMMSException {

        log.info("Generating mandate request ===============");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            setDate(mandate, dateFormat.parse(emandateRequest.getStartDate()), dateFormat.parse(emandateRequest.getEndDate()));
        }catch (Exception e)
        {
            log.error("Exception trace {}",e);
            throw new CMMSException(EmandateResponseCode.PARSE_DATE_ERROR.getValue(),"500","500");
        }
        mandate.setEmail(emandateRequest.getEmailAddress());
        mandate.setAccountNumber(emandateRequest.getAccountNumber());
        //TODO make sure you remove Optional.ofNullable when deploying to the server
        //TODO add this one
        String accountName =null;
        try {
            accountName = EMandateValidator.generateAccountName(emandateRequest).getAccountName();
        }catch (CMMSException e)
        {
            throw new CMMSException(e.getMessage(),"404",EmandateResponseCode.INVALID_ACCOUNT_NUMBER.getCode());
        }
        if (accountName ==null)
        {
            throw new CMMSException("Wrong account number","400",EmandateResponseCode.INVALID_ACCOUNT_NUMBER.getCode());
        }
        mandate.setAccountName(accountName);
        mandate.setBank(new Bank(bank.getId(),bank.getCode(),bank.getName()));   //subscriber's bank
        mandate.setLastActionBy(null);
        mandate.setLastActionBy(null);
//        mandate.setLastActionBy(new User(userOperator.getId(), userOperator.getName(), userOperator.getEmailAddress(), userOperator.isActivated(), userOperator.getEntityType()));
        mandate.setPayerName(emandateRequest.getPayerName());
        mandate.setPayerAddress(emandateRequest.getPayerAddress());
        mandate.setProduct(new Product(product.getId(),product.getName(),product.getAmount(),product.getDescription()));
        mandate.setPhoneNumber(emandateRequest.getPhoneNumber());
        mandate.setNarration(emandateRequest.getNarration());
        /**I refactored the this portion which reappeared on other sections of the code*/
        setMandateDebit(mandate, emandateRequest.isFixedAmountMandate(),emandateRequest.getAmount(),emandateRequest.getFrequency());
        try {
            setDate(mandate, dateFormat.parse(emandateRequest.getStartDate()), dateFormat.parse(emandateRequest.getEndDate()));
        }catch (Exception e)
        {
            log.error("Exception trace {}",e);
            throw new CMMSException(EmandateResponseCode.PARSE_DATE_ERROR.getValue(),"500","500");
        }
        mandate.setChannel(channel);
        mandate.setBiller(new Biller(biller.getId(),biller.getName(),biller.getRcNumber(),biller.getAccountNumber(),biller.getDescription(),biller.getAccountName()));
        mandate.setServiceType(ServiceType.PREPAID);
        mandate.setFixedAmountMandate(emandateRequest.isFixedAmountMandate());
        MandateStatus mandateStatus = null;
        mandateStatus = mandateStatusService.getMandateStatusByStatusName(MandateStatusType.BANK_APPROVE_MANDATE);
        if (mandateStatus==null)
        {
            log.info("Failed: Could not retrieve mandate status from DB");
            return null;
        }
        mandate.setStatus(mandateStatus);
        mandate.setWorkflowStatus(mandateStatus.getName());
        mandate.setSubscriberCode(emandateRequest.getSubscriberCode());
        mandate.setRequestStatus(Constants.STATUS_ACTIVE);
        mandate.setMandateCode(mandateCode);
        mandate.setRejection(null);
        mandate.setBankNotified(1);
        mandate.setBillerNotified(1);
        mandate.setCreatedBy(null);
        log.info("Generated mandate request: "+mandate);
        return mandate;
    }

    public void setMandateDebit(Mandate mandate, boolean fixed, BigDecimal amount,int freq) {
        if (fixed)
        {
            mandate.setAmount(amount);
            mandate.setFrequency(freq);
            mandate.setMandateType(MandateRequestType.FIXED);
            if (mandate.getFrequency() > 0) {
                Date nextDebitDate = DateUtils.calculateNextDebitDate(mandate.getStartDate(), mandate.getEndDate(),
                        mandate.getFrequency());
                mandate.setNextDebitDate(nextDebitDate == null ? DateUtils.lastSecondOftheDay(mandate.getEndDate())
                        : DateUtils.nullifyTime(nextDebitDate));
            }

        }else
        {
            mandate.setMandateType(MandateRequestType.VARIABLE);
            mandate.setFrequency(0);
            mandate.setVariableAmount(amount);
        }
    }


    public Mandate generateMRC(Mandate mandate, Subscriber subscriber, MRCMandateRequest emandateRequest,
                               String mandateCode, Product product,Biller biller, Channel channel) throws CMMSException {

        log.info("Generating mandate request ===============");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

       try {
           setDate(mandate, dateFormat.parse(emandateRequest.getStartDate()), dateFormat.parse(emandateRequest.getEndDate()));
       }catch (Exception e)
       {
           log.error("Exception trace {}",e);
           throw new CMMSException(EmandateResponseCode.PARSE_DATE_ERROR.getValue(),"500","500");
       }
        mandate.setEmail(subscriber.getEmail());
        mandate.setAccountNumber(subscriber.getAccountNumber());
        //TODO make sure you remove Optional.ofNullable when deploying to the server
        //TODO add this one
        mandate.setAccountName(subscriber.getAccountName());
        Bank bank = subscriber.getBank();
        mandate.setBank(new Bank(bank.getId(),bank.getCode(),bank.getName()));   //subscriber's bank
        mandate.setLastActionBy(null);
        mandate.setLastActionBy(null);
//        mandate.setLastActionBy(new User(userOperator.getId(), userOperator.getName(), userOperator.getEmailAddress(), userOperator.isActivated(), userOperator.getEntityType()));
        mandate.setPayerName(subscriber.getPayerName());
        mandate.setPayerAddress(subscriber.getPayerAddress());
        mandate.setProduct(new Product(product.getId(),product.getName(),product.getAmount(),product.getDescription()));
        mandate.setPhoneNumber(subscriber.getPhoneNumber());
        mandate.setNarration(emandateRequest.getNarration());
        /**I refactored the this portion which reappeared on other sections of the code*/
        setMandateDebit(mandate, emandateRequest.isFixedAmountMandate(),emandateRequest.getAmount(),emandateRequest.getFrequency());
        mandate.setChannel(channel);
        mandate.setBiller(new Biller(biller.getId(),biller.getName(),biller.getRcNumber(),biller.getAccountNumber(),biller.getDescription(),biller.getAccountName()));
        mandate.setServiceType(ServiceType.PREPAID);
        mandate.setFixedAmountMandate(emandateRequest.isFixedAmountMandate());
        MandateStatus mandateStatus = null;
        mandateStatus = mandateStatusService.getMandateStatusByStatusName(MandateStatusType.BANK_APPROVE_MANDATE);
        if (mandateStatus==null)
        {
            log.info("Failed: Could not retrieve mandate status from DB");
            return null;
        }
        mandate.setStatus(mandateStatus);
        mandate.setWorkflowStatus(mandateStatus.getName());
        mandate.setSubscriberCode(emandateRequest.getSubscriberCode());
        mandate.setRequestStatus(Constants.STATUS_ACTIVE);
        mandate.setMandateCode(mandateCode);
        mandate.setRejection(null);
        mandate.setBankNotified(1);
        mandate.setBillerNotified(1);
        mandate.setCreatedBy(null);
        log.info("Generated mandate request: "+mandate);
        return mandate;
    }


    public ResponseEntity<?> saveToRedisCache(BigDecimal amount, String sessionId, Mandate mandate) throws CMMSException {
        GenerateOTPResponse generatOTPResponse;
        OTPResponse otpResponse;
        boolean saved = redisService.saveObjToCache(mandate, sessionId);
        if (saved) {
            log.info("Object saved with sessionId : "+sessionId);
            generatOTPResponse = (GenerateOTPResponse) OTPService.setupOTPRequest(mandate.getMandateCode(), mandate.getBank().getCode(), String.valueOf(amount), null, OTPMethodName.GENERATE_OTP);
        }else
        {
            log.info("Object was not cached in redis");
            otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.UNKNOWN,sessionId,"");
            return ResponseEntity.ok(otpResponse);
        }
        if (StringUtils.isEmpty(generatOTPResponse.getResponseCode()))
        {
            log.info("Generate OTP response code is null");
            otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.UNKNOWN,sessionId,"");
            return ResponseEntity.ok(otpResponse);
        }
        return OTPService.getOTPResponseEntity(sessionId, generatOTPResponse.getResponseCode());
    }

    public void setDate(Mandate mandate, Date parse, Date parse2) throws CMMSException {
        mandate.setStartDate(parse);
        log.info("Mandate start date: " + mandate.getStartDate());
        mandate.setEndDate(parse2);
        log.info("Mandate start date: " + mandate.getEndDate());
    }


    public EmandateResponse authenticateSubscriber(MRCMandateRequest requestObject, Subscriber subscriber) {
        if (!subscriber.isActivated())
            return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MRC_DEACTIVATED, null,String.valueOf(requestObject.getMandateReferenceCode()));

        if (requestObject.getMrcPin() == null || !EncyptionUtil.doSHA512Encryption(requestObject.getMrcPin(), salt).equals(subscriber.getMrcPin()))
            return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.INVALID_MRC_PIN, null,String.valueOf(requestObject.getMandateReferenceCode()));
        return null;
    }

    public static void main(String[] args) {
//
    }
}
