package ng.upperlink.nibss.cmms.service.emandate;

import ng.upperlink.nibss.cmms.dto.account.request.AccountRequest;
import ng.upperlink.nibss.cmms.dto.emandates.otp.OTPResponse;
import ng.upperlink.nibss.cmms.dto.emandates.response.EmandateResponse;
import ng.upperlink.nibss.cmms.dto.emandates.subscriber.*;
import ng.upperlink.nibss.cmms.enums.Channel;
import ng.upperlink.nibss.cmms.enums.emandate.EmandateResponseCode;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.emandate.Subscriber;
import ng.upperlink.nibss.cmms.model.bank.Bank;
import ng.upperlink.nibss.cmms.repo.emandate.SubscriberRepo;
import ng.upperlink.nibss.cmms.service.account.AccountValidationService;
import ng.upperlink.nibss.cmms.util.email.SmtpMailSender;
import ng.upperlink.nibss.cmms.util.emandate.EmandateFormValidation;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class SubscriberService {
    private static Logger logger = LoggerFactory.getLogger(SubscriberService.class);

    @Value("${encryption.salt}")
    private String salt;
    @Value("${defaultPasswordLength}")
    private int defaultPasswordLength;
    @Value("${email_from}")
    private String fromEmail;

    private String amount = "1";
    private SmtpMailSender smtpMailSender;

    private SubscriberRepo subscriberRepo;

    private EmandateBaseService emandateBaseService;
    EmandateFormValidation emandateFormValidation;

    @Autowired
    public void setSmtpMailSender(SmtpMailSender smtpMailSender) {
        this.smtpMailSender = smtpMailSender;
    }

    @Autowired
    public void setEmandateFormValidation(EmandateFormValidation emandateFormValidation) {
        this.emandateFormValidation = emandateFormValidation;
    }
    @Autowired
    public void setEmandateBaseService(EmandateBaseService emandateBaseService) {
        this.emandateBaseService = emandateBaseService;
    }

    @Autowired
    public void setSubscriberRepoRepo(SubscriberRepo agentManagerRepo) {
        this.subscriberRepo = agentManagerRepo;
    }

    public Page<Subscriber> getAll(Pageable pageable){
        return subscriberRepo.getAll(pageable);
    }

    public Page<Subscriber> getAllByBank(Long bankId,boolean activated,Pageable pageable){
        return subscriberRepo.getAllByBank(bankId,activated,pageable);
    }

    public Page<Subscriber> getAllActivated(boolean activated,Pageable pageable){
        return subscriberRepo.getAllByActivated(activated,pageable);
    }

    public ResponseEntity setUpMRC(SubscriberRequestBody request,String sessionId) {
        OTPResponse otpResponse = null;
        EmandateResponse emandateResponse = null;

        SubscriberRequest subscriberRequest = request.getRequest();

        Subscriber subscriber = new Subscriber();
        Bank bank =null;

        if (subscriberRequest ==null)
        {
            emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.INVALID_REQUEST,null,"");
            return ResponseEntity.badRequest().body(emandateResponse);
        }
        try {
            validate(subscriberRequest);
            bank = emandateBaseService.authenticateBank(request.getAuthParam());
            subscriber = generate(subscriber,subscriberRequest,bank);
            /**This part takes care of the request coming from thirdParty app*/
            /*GenerateOTPResponse getGeneratOTPResponse = OTPService.setupOTPRequest(subscriber.getMrc(), subscriber.getBank().getCode(), amount, null, OTPMethodName.GENERATE_OTP, GenerateOTPResponse.class);
            if (getGeneratOTPResponse ==null)
            {
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.UNKNOWN,sessionId,"");
                return ResponseEntity.ok(otpResponse);
            }
            return OTPService.getOTPResponseEntity(sessionId, getGeneratOTPResponse.getResponseCode());*/
            return performSaveOperation(subscriber);
        } catch (CMMSException e) {
            logger.error("CMMSException trace {}",e);
            emandateResponse =new EmandateResponse(e.getEmandateErrorCode(),null,e.getMessage());
            return ResponseEntity.status(Integer.valueOf(e.getCode())).body(emandateResponse);
        }
    }

    public ResponseEntity<?> performSaveOperation(Subscriber subscriber) {
        EmandateResponse emandateResponse;
        try
        {
            subscriber = subscriberRepo.save(subscriber);
            emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.CREATION_SUCCESSFUL,subscriber.getMrc(),"");
            sendAwarenessMail(subscriber,subscriber.getBank().getName());
            return ResponseEntity.ok(emandateResponse);
        }catch (Exception e)
        {
            logger.error("Excption trace { }",e);
            emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,null,"");
            return ResponseEntity.ok(emandateResponse);
        }
    }

    public ResponseEntity processToggle(MRCRequest request)
    {

        EmandateResponse emandateResponse =null;
        try {
            Bank bank = emandateBaseService.authenticateBank(request.getAuthParam());
            Subscriber toggleSubscriber = toggle(request.getMrc(),bank);
            emandateResponse = new EmandateResponse(EmandateResponseCode.CREATION_SUCCESSFUL.getCode(),toggleSubscriber.getMrc(),"Toggle successful");
            return ResponseEntity.ok(emandateResponse);
        } catch (CMMSException e) {
            logger.error("CMMSException trace {}",e);
            emandateResponse =new EmandateResponse(e.getEmandateErrorCode(),request.getMrc(),e.getMessage());
            return ResponseEntity.status(Integer.valueOf(e.getCode())).body(emandateResponse);
        }
    }
    public ResponseEntity<?> getByMRC(MRCRequest request){
        try {
            emandateBaseService.authenticateBank(request.getAuthParam());
        } catch (CMMSException e) {
            e.printStackTrace();
            logger.error("CMMSException {} ",e);
            return ResponseEntity.ok(new EmandateResponse(e.getEmandateErrorCode(),request.getMrc(),e.getMessage()));

        }
        Subscriber su = subscriberRepo.getByMRC(request.getMrc());
        if (su ==null)
            return ResponseEntity.ok(EmandateBaseService.generateEmandateResponse(EmandateResponseCode.INVALID_MRC,request.getMrc(),""));
        SubscriberResponse response = generateSubscriberResponse(su);
        return ResponseEntity.ok(response);
    }

    public SubscriberResponse generateSubscriberResponse(Subscriber su) {
        SubscriberResponse response = new SubscriberResponse();
        response.setAccountNumber(su.getAccountNumber());
        response.setAddress(su.getPayerAddress());
        response.setBankName(su.getBank().getName());
        response.setBvn(su.getBvn());
        response.setEmail(su.getEmail());
        response.setPayerName(su.getPayerName());
        response.setPhoneNumber(su.getPhoneNumber());
        response.setMandateReferenceCode(su.getMrc());
        if (su.isActivated())
            response.setStatus("Active");
        else response.setStatus("Inactive");
        return response;
    }

    public Subscriber save(Subscriber subscriber){
        return subscriberRepo.save(subscriber);
    }
    private void validate(SubscriberRequest request) throws CMMSException {

        long accountNumberCount = 0;
        boolean b = emandateFormValidation.validAccountNumber(request.getAccountNumber());

        Optional<String> e2 = Optional.ofNullable(emandateFormValidation.validateContactDetails(request.getPhoneNumber(), request.getEmail(), request.getPayerName(), request.getPayerAddress()));

        accountNumberCount = subscriberRepo.getCountAccountNumbers(request.getAccountNumber());
        if (accountNumberCount > 0)
            throw new CMMSException(EmandateResponseCode.ACCOUNT_NUMBER_EXIST.getValue().replace("{}",request.getAccountNumber()),"400",EmandateResponseCode.ACCOUNT_NUMBER_EXIST.getCode());
        List<String> errors = new ArrayList<>();

        if (!emandateFormValidation.validBVN(request.getBvn()))
            errors.add("Invalid BVN "+request.getBvn()+" provided");
        if (!emandateFormValidation.validNumber(request.getPassword())||!emandateFormValidation.validLength(request.getPassword(),4))
            errors.add("Invalid MRC Pin "+request.getPassword()+" provided,Pin must be 4 digits");
        if (!emandateFormValidation.validLength(request.getChannelCode(),2) || !emandateFormValidation.validNumber(request.getChannelCode()))
            errors.add("Invalid channel code "+request.getChannelCode());

        if (!b) {
            errors.add("Invalid account number, Account number must be of 10 digits only");
            throw new CMMSException("Invalid account number, Account number must be of 10 digits only","400",EmandateResponseCode.INVALID_ACCOUNT_INFO.getCode());
        }
        if (e2.isPresent()) {
            errors.add(e2.get());
            throw new CMMSException(e2.get(),"400",EmandateResponseCode.INVALID_SUBSCRIBER_INFO.getCode());
        }
        if (!errors.isEmpty())
            throw new CMMSException(errors.toString(),"400",EmandateResponseCode.INVALID_REQUEST.getCode());
    }

    private Subscriber generate(Subscriber subscriber, SubscriberRequest request,Bank bank) throws CMMSException {

        subscriber.setActivated(true);
        subscriber.setAccountNumber(request.getAccountNumber());
        subscriber.setBank(new Bank(bank.getId(),bank.getCode(),bank.getName(),bank.getApiKey()));
        subscriber.setBvn(request.getBvn());
        subscriber.setChannel(Channel.findById(request.getChannelCode()));
        subscriber.setEmail(request.getEmail());
        subscriber.setMrcPin(EncyptionUtil.doSHA512Encryption(request.getPassword(), salt));
        subscriber.setPayerAddress(request.getPayerAddress());
        subscriber.setPayerName(request.getPayerName());
        subscriber.setPhoneNumber(request.getPhoneNumber());
        String mrc = RandomStringUtils.random(this.defaultPasswordLength,false,true);
        subscriber.setMrc(mrc);
        String accountName = AccountValidationService.request(new AccountRequest(bank.getCode(),request.getAccountNumber())).getAccountName();

        if (accountName ==null)
        {
            throw new CMMSException("Wrong account number","400",EmandateResponseCode.INVALID_ACCOUNT_NUMBER.getCode());
        }
        subscriber.setAccountName(accountName);
        return subscriber;
    }
    private Subscriber toggle(String mrc,Bank bank) throws CMMSException {

        Subscriber subscriber = subscriberRepo.getByMRC(mrc);
        if (subscriber ==null)
            throw new CMMSException(EmandateResponseCode.INVALID_MRC.getValue().replace("{}",mrc),"404",EmandateResponseCode.INVALID_MRC.getCode());
        if (subscriber.getBank().getId() !=bank.getId())
            throw new CMMSException(EmandateResponseCode.UNAUTHORIZED.getValue().replace("{}","owner's bank"),"401",EmandateResponseCode.UNAUTHORIZED.getCode());
        boolean activated = subscriber.isActivated();
        subscriber.setActivated(!activated);
        try
        {
            subscriber = save(subscriber);
        }catch (Exception e)
        {
            logger.error("Exception trace {}",e);
            throw new CMMSException(EmandateResponseCode.MANDATE_NOT_GENERATEED.getValue(),"500",EmandateResponseCode.MANDATE_NOT_GENERATEED.getCode());
        }

        return subscriber;
    }

    public Subscriber getByCode(String code){
        return subscriberRepo.getByMRC(code);
    }

    private void sendAwarenessMail(Subscriber subscriber, String bankName) {

        String emailAddress = subscriber.getEmail();
        String[] email = {emailAddress};

        if (email.length > 0) {
            String subject = "MRC Setup ";
            String title = "Mandate Reference Code Setup";
            String message = "Your universal Mandate Reference Code has been successfully setup on Central Mandate Management System(CMMS). Please find the details below.";

            //Send the mail
            smtpMailSender.sendMail(fromEmail, email, subject,
                    title, message, generateDetails(subscriber,bankName));
        }

    }

    private String generateDetails(Subscriber subscriber,String bankName) {

        String details = "";
        details += "<p><i>Date :</i> " + subscriber.getCreatedAt() + "</p>";
        details += "<p><i>Registered Email :</i> " + subscriber.getEmail() + "</p>";
        details += "<p><strong>Mandate Reference Code (MRC) :" + subscriber.getMrc() + "</strong> </p>";
        details += "<strong>Details of the account tied to this MRC</strong>";
        details += "<p>Account Name:</p><strong> "+subscriber.getAccountName()+"</strong>";
        details += "<p>Account Number:</p><strong> "+subscriber.getAccountNumber()+"</strong>";
        details += "<p>Bank :  </p><strong> "+bankName+"</strong>";
        details += "<p>Please note that the MRC can be can be used to activate mandates across other billers</p>";
        return details;
    }
    public long count() {
        synchronized (new Object()) {
            return subscriberRepo.count();
        }
    }

    public long getCountByBank(Long bankId)
    {
        return subscriberRepo.getCountByBank(bankId);
    }

    public long getCountBy(boolean status)
    {
        return subscriberRepo.getCountByStatus(status);
    }


}
