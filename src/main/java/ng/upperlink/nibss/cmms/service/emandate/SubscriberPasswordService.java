package ng.upperlink.nibss.cmms.service.emandate;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.emandates.AuthParam;
import ng.upperlink.nibss.cmms.dto.emandates.response.EmandateResponse;
import ng.upperlink.nibss.cmms.dto.emandates.subscriber.MRCRequest;
import ng.upperlink.nibss.cmms.dto.emandates.subscriber.SubscriberNewPinREquest;
import ng.upperlink.nibss.cmms.enums.ServiceResponseCode;
import ng.upperlink.nibss.cmms.enums.UserType;
import ng.upperlink.nibss.cmms.enums.emandate.EmandateResponseCode;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.bank.Bank;
import ng.upperlink.nibss.cmms.model.emandate.Subscriber;
import ng.upperlink.nibss.cmms.model.emandate.MrcRecoveryCode;
import ng.upperlink.nibss.cmms.repo.emandate.MrcPasswordRepo;
import ng.upperlink.nibss.cmms.util.email.SmtpMailSender;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SubscriberPasswordService {
    private SubscriberService subscriberService;
    private EmandateBaseService emandateBaseService;
    private MrcPasswordRepo mrcPasswordRepo;
    @Value("${email_from}")
    private String fromEmail;
    @Value("${encryption.salt}")
    private String salt;

    private SmtpMailSender smtpMailSender;

    @Autowired
    private void setSmtpMailSender(SmtpMailSender smtpMailSender) {
        this.smtpMailSender = smtpMailSender;
    }

    @Autowired
    private void setSubscriberPasswordRepo(MrcPasswordRepo srcPasswordRepo) {
        this.mrcPasswordRepo = srcPasswordRepo;
    }

    @Autowired
    private void setEmandateBaseService(EmandateBaseService emandateBaseService) {
        this.emandateBaseService = emandateBaseService;
    }

    @Autowired
    private void setSubscriberService(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    public ResponseEntity<?> generateRecoveryCode(MRCRequest request, UserType userType) {
        Subscriber subscriber = subscriberService.getByCode(request.getMrc());
        MrcRecoveryCode mrcRecoveryCode =null;
        EmandateResponse emandateResponse = null;
        if (subscriber ==null)

        {
            emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.INVALID_MRC, request.getMrc(), request.getMrc());
            return ResponseEntity.status(404).body(emandateResponse);
        }
        if (!subscriber.isActivated())
        {
            emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MRC_DEACTIVATED,request.getMrc(),request.getMrc());
            return ResponseEntity.ok(emandateResponse);
        }
        String recoveryCode = EncyptionUtil.generateString(5, false, true);
        try {
            Bank bank = authenticate(request.getAuthParam(), userType);
            if (subscriber.getBank().getId() !=bank.getId())

            {
                emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.UNAUTHORIZED_MRC_UPDATE, null, "");
                return ResponseEntity.status(404).body(emandateResponse);
            }
            mrcRecoveryCode = save(recoveryCode, subscriber);
        } catch (CMMSException e) {
            e.printStackTrace();
            emandateResponse = new EmandateResponse(e.getEmandateErrorCode(), null,e.getMessage() );
            return ResponseEntity.status(Integer.valueOf(e.getCode())).body(emandateResponse);
        }

        if (mrcRecoveryCode == null)
        {
            emandateResponse = new EmandateResponse(EmandateResponseCode.UNKNOWN.getCode(), null, ServiceResponseCode.PASSWORD_RECOVERY_CODE_ERROR_MSG);
            return ResponseEntity.status(401).body(emandateResponse);
        }

        sendRecoveryEmail(subscriber, recoveryCode);
        emandateResponse = new EmandateResponse("00",null,"Recovery code has been sent to your registered mail");
        return ResponseEntity.ok(emandateResponse);
    }

    private Bank authenticate(AuthParam authParam, UserType userType) throws CMMSException {
        EmandateResponse emandateResponse;
        switch (userType)
        {
            case BANK:
                    return emandateBaseService.authenticateBank(authParam);

//            case BILLER:
//                emandateBaseService.authenticateBiller(request.getAuthParam());
//                mrcRecoveryCode = save(recoveryCode,new Subscriber(subscriber.getId(),subscriber.getMrc(),subscriber.getEmail(),subscriber.getPayerName(),subscriber.getBank()));
//
//                break;
            default:
                throw new CMMSException(EmandateResponseCode.UNAUTHORIZED.getValue(),"401",EmandateResponseCode.UNAUTHORIZED.getCode());
        }
    }

    public ResponseEntity<?> confirmRecoveryCode(SubscriberNewPinREquest request, UserType userType) {
        MrcRecoveryCode mrcRecoveryCode =null;
        EmandateResponse emandateResponse =null;
        try {
            Bank bank = authenticate(request.getAuthParam(), userType);
            mrcRecoveryCode = findRecoveryCode(request.getRecoveryCode());

            if (mrcRecoveryCode == null) {
                emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.INVALID_RECOVERY_CODE, null, "");
                return ResponseEntity.status(404).body(emandateResponse);
            }
            // check if this recovery code has been used before
            if (mrcRecoveryCode.isStatus()) {
                emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.RECOVERY_CODE_USED, null, "");
                return ResponseEntity.status(400).body(emandateResponse);
            }
            if (checkValidityPeriod(mrcRecoveryCode.getCreatedAt()) >= 1) {
                emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.RECOVERY_CODE_EXPIRED, null, "");
                return ResponseEntity.status(400).body(emandateResponse);
            }
            if (mrcRecoveryCode.getSubscriber().getBank().getId() !=bank.getId())

            {
                emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.UNAUTHORIZED_MRC_UPDATE, null, "");
                return ResponseEntity.status(404).body(emandateResponse);
            }
            updateRecoveryCode(mrcRecoveryCode);
            return updatePassword(request, mrcRecoveryCode.getSubscriber().getMrc());
        } catch (CMMSException e) {
            e.printStackTrace();
            emandateResponse = new EmandateResponse(e.getEmandateErrorCode(), null, e.getMessage());
            return ResponseEntity.status(Integer.valueOf(e.getCode())).body(emandateResponse);
        }
    }
    private MrcRecoveryCode save(String recoveryCode, Subscriber subscriber) throws CMMSException {
        try {
            MrcRecoveryCode mrcRecoveryCode = new MrcRecoveryCode();
            mrcRecoveryCode.setRecoveryCode(recoveryCode);
            mrcRecoveryCode.setSubscriber(new Subscriber(subscriber.getId(),subscriber.getMrc(),subscriber.getEmail()));
            return mrcPasswordRepo.saveAndFlush(mrcRecoveryCode);
        } catch (Exception e) {
            log.error("Unable to save mrcPin recovery code", e);
            throw new CMMSException(EmandateResponseCode.UNKNOWN.getValue(),"500","500");
        }
    }

    private MrcRecoveryCode findRecoveryCode(String recoveryCode) throws CMMSException {
        try {
            return mrcPasswordRepo.findRecoveryCode(recoveryCode);
        } catch (Exception e) {
            log.error("Unable to find recovery code {}", recoveryCode, e);
            throw new CMMSException("Recovery code: "+recoveryCode+" does not exist","404","404");
        }
    }
    private void sendRecoveryEmail(Subscriber subscriber, String recoveryCode) {

        String emailAddress = subscriber.getEmail();
        String[] email = {emailAddress};

        if (email.length > 0) {
            String subject = "MRC PIN RESET ";
            String title = "Mandate Reference Code Pin Reset";
            String message = "Pin reset has been initiated against your Mandate Reference Code: "+subscriber.getMrc();
            Date date = new Date();
            String details = "";
            details += "<p><i>Date :</i> " + date + "</p>";
            details += "<p>PIN Recovery Code:</p><strong> "+recoveryCode+"</strong>";
            details += "<p>Kindly copy the code to continue</p>";
            details += "<p>Valid within 24 hrs</p>";
            //Send the mail
            smtpMailSender.sendMail(fromEmail, email, subject,
                    title, message, details);
        }

    }
    private long checkValidityPeriod(Date createdDate) {
        long diffInMillies = Math.abs(new Date().getTime() - createdDate.getTime());
        return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }
    private void updateRecoveryCode(MrcRecoveryCode passwordRecoveryCode) {
        try {
            passwordRecoveryCode.setUpdatedAt(new Date());
            passwordRecoveryCode.setStatus(true);
            mrcPasswordRepo.saveAndFlush(passwordRecoveryCode);
        } catch (Exception e) {
            log.error("Exception Unable to update password recovery code {}", passwordRecoveryCode, e);
        }
    }
    private ResponseEntity<?> updatePassword (SubscriberNewPinREquest request,String mrc) throws CMMSException {

        try {
            Subscriber subscriber = subscriberService.getByCode(mrc);
            if (subscriber ==null)
            {
                throw new CMMSException(EmandateResponseCode.INVALID_MRC.getValue().replace("{}",mrc),"404","404");
            }
            subscriber.setMrcPin(EncyptionUtil.doSHA512Encryption(request.getNewPin(), salt));
           subscriber = subscriberService.save(subscriber);
            return ResponseEntity.ok(new EmandateResponse("00",subscriber.getMrc(),"Pin reset was successful"));
        }catch (Exception e)
        {
            log.error("Save error {}",e);
            return ResponseEntity.status(500).body(new EmandateResponse(EmandateResponseCode.UNKNOWN.getCode(),null,"Pin reset was successful"));
        }

    }
}
