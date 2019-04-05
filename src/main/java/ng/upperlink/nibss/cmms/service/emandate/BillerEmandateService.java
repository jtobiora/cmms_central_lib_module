package ng.upperlink.nibss.cmms.service.emandate;


import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.emandates.EmandateRequest;
import ng.upperlink.nibss.cmms.dto.emandates.MRCMandateRequest;
import ng.upperlink.nibss.cmms.dto.emandates.otp.generateOTPRequestResponse;
import ng.upperlink.nibss.cmms.dto.emandates.otp.OTPResponse;
import ng.upperlink.nibss.cmms.dto.emandates.response.EmandateResponse;
import ng.upperlink.nibss.cmms.enums.*;
import ng.upperlink.nibss.cmms.enums.emandate.EmandateResponseCode;

import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.emandate.Subscriber;
import ng.upperlink.nibss.cmms.model.bank.Bank;
import ng.upperlink.nibss.cmms.model.biller.Biller;
import ng.upperlink.nibss.cmms.model.biller.Product;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.repo.MandateRepo;
import ng.upperlink.nibss.cmms.service.bank.BankService;
import ng.upperlink.nibss.cmms.service.biller.ProductService;
import ng.upperlink.nibss.cmms.service.emandate.auth.McashAutheticationService;
import ng.upperlink.nibss.cmms.service.emandate.auth.OTPService;
import ng.upperlink.nibss.cmms.service.mandateImpl.MandateStatusService;
import ng.upperlink.nibss.cmms.util.MandateUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BillerEmandateService
{
    private ProductService productService;
    private BankService bankService;
    private MandateRepo mandateRepo ;
    private MandateStatusService mandateStatusService;
    private McashAutheticationService mcashAutheticationService;
    private EmandateBaseService emandateBaseService;
    private SubscriberService subscriberService;
    private SendMandateAdvice sendMandateAdvice;


    @Autowired
    public void setSendMandateAdvice(SendMandateAdvice sendMandateAdvice) {
        this.sendMandateAdvice = sendMandateAdvice;
    }


    @Autowired
    public void setSubscriberService(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @Autowired
    public void setEmandateBaseService(EmandateBaseService emandateBaseService) {
        this.emandateBaseService = emandateBaseService;
    }
    @Autowired
    public void setMcashAutheticationService(McashAutheticationService mcashAutheticationService)
    {
        this.mcashAutheticationService = mcashAutheticationService;
    }
    @Autowired
    public void setMandateStatusService(MandateStatusService mandateStatusService){
        this.mandateStatusService = mandateStatusService;
    }
    @Autowired
    public void setBankService(BankService bankService) {
        this.bankService = bankService;
    }
    @Autowired
    public void setProductService(ProductService productService)
    {
        this.productService = productService;
    }
    @Autowired
    public void setMandateRepo(MandateRepo mandateRepo)
    {
        this.mandateRepo = mandateRepo;
    }


    public ResponseEntity<?> processMandate(EmandateRequest requestObject, String sessionId){
        Mandate mandate = null;
        OTPResponse otpResponse =null;
        generateOTPRequestResponse generateOTPRequestResponse = null;
        try
        {
            Product product = productService.getProductById(requestObject.getProductId());
            if (product==null)
            {
                log.info(EmandateResponseCode.PRODUCT_NOT_FOUND.getValue().replace("{}",String.valueOf(requestObject.getProductId())));
                otpResponse= OTPService.generateOTPResponse(EmandateResponseCode.PRODUCT_NOT_FOUND,sessionId,String.valueOf(requestObject.getProductId()));
                return ResponseEntity.ok(otpResponse);
            }
            Biller biller = product.getBiller();
            if (biller==null)
            {
                log.info("Could not find the Biller of the product: "+product.getName());
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.UNKNOWN,sessionId,String.valueOf(requestObject.getProductId()));
                return ResponseEntity.ok(otpResponse);
            }
            String rcNumber = biller.getRcNumber();
            Bank subscriberBank = bankService.getBankByCode(requestObject.getBankCode());
            if (subscriberBank == null)
            {
                log.info(EmandateResponseCode.SUBSCRIBER_BANK_NOT_FOUND.getValue()+" CBN code:"+requestObject.getBankCode());
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.SUBSCRIBER_BANK_NOT_FOUND,sessionId,requestObject.getBankCode());
                return ResponseEntity.ok(otpResponse);
            }
            String mandateCode = MandateUtils.getMandateCode(String.valueOf(System.currentTimeMillis()), rcNumber, String.valueOf(product.getId()));
            log.info("Generated mandate code "+mandateCode );
            Channel channel = null;
            channel = Channel.findById(requestObject.getChannelCode());

            if (channel ==null || channel.equals(Channel.UNKNOWN))
            {
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.CHANNEL_NOT_FOUND,sessionId,requestObject.getChannelCode());
                return ResponseEntity.ok(otpResponse);
            }
            log.info("The Request is Coming through channel : "+channel);
            mandate = emandateBaseService.generateMandate(new Mandate(), requestObject, mandateCode, product, subscriberBank,biller,channel);
            if (mandate ==null)
            {
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,sessionId,"");
                return ResponseEntity.ok(otpResponse);
            }

            /**Cache mandate object in Redis and make a call to OTP service*/
            return emandateBaseService.saveToRedisCache(requestObject.getAmount(), sessionId, mandate);
//            EmandateResponse emandateResponse = makeMcashCallNoMRC(requestObject, mandate, subscriberBank, channel);
//            if (emandateResponse != null) return emandateResponse;

//            return saveOperation(mandate);
        } catch (CMMSException ex) {
            log.error("---Exception trace --- {} ", ex);
           otpResponse = new OTPResponse(ex.getMessage(),sessionId,ex.getEmandateErrorCode());
            return ResponseEntity.ok(otpResponse);
        }
    }

    public EmandateResponse makeMcashCallNoMRC(EmandateRequest requestObject, Mandate mandate, Bank subscriberBank, Channel channel) {
        EmandateResponse emandateResponse = mcashAutheticationService.setupMcashRequest(mandate,"requestObject.getSubscriberPassCode()",channel.getValue());
        log.info("Mcash response : "+emandateResponse);
        if (StringUtils.isEmpty(emandateResponse.getResponseCode()))
            return  EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MCASH_NO_RESPONSE,null,subscriberBank.getName());
        if (!emandateResponse.getResponseCode().equalsIgnoreCase("00"))
            {
                emandateResponse.setMandateCode(null);
                return emandateResponse;
            }
        return emandateResponse;
    }

    public ResponseEntity<?> processMandateWithMRC(MRCMandateRequest requestObject, String sessionId){
        Mandate mandate = null;
        Subscriber subscriber =null;
        generateOTPRequestResponse generateOTPRequestResponse = null;
        OTPResponse otpResponse = null;
        try
        {
            Product product = productService.getProductById(requestObject.getProductId());
            if (product==null)
            {
                log.info(EmandateResponseCode.PRODUCT_NOT_FOUND.getValue().replace("{}",String.valueOf(requestObject.getProductId())));
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.PRODUCT_NOT_FOUND,sessionId,String.valueOf(requestObject.getProductId()));
                return ResponseEntity.ok(otpResponse);
            }
            Biller biller = product.getBiller();
            if (biller==null)
            {
                    log.info("Could not find the Biller of the product: "+product.getName());
                    otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.UNKNOWN,sessionId,String.valueOf(requestObject.getProductId()));
                    return ResponseEntity.ok(otpResponse);

            }
            String rcNumber = biller.getRcNumber();
//            Bank subscriberBank = bankService.getBankByCode(requestObject.getBankCode());
//            if (subscriberBank == null)
//            {
//                log.info(EmandateResponseCode.SUBSCRIBER_BANK_NOT_FOUND.getValue()+" CBN code:"+requestObject.getBankCode());
//                return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.SUBSCRIBER_BANK_NOT_FOUND,null,requestObject.getBankCode());
//            }

            /**
             * Get the subscriber using the MRC */
            subscriber = subscriberService.getByCode(requestObject.getMandateReferenceCode());
            if (subscriber ==null)
            {
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.INVALID_MRC, sessionId,String.valueOf(requestObject.getMandateReferenceCode()));
                return ResponseEntity.ok(otpResponse);
            }

            EmandateResponse x = emandateBaseService.authenticateSubscriber(requestObject, subscriber);
            if (x != null)
            {
                otpResponse = new OTPResponse(x.getResponseCode(),sessionId,x.getResponseDescription());
                return ResponseEntity.ok(otpResponse);
            }

            String mandateCode = MandateUtils.getMandateCode(String.valueOf(System.currentTimeMillis()), rcNumber, String.valueOf(product.getId()));
            log.info("Generated mandate code "+mandateCode );
            Channel channel = null;
            channel = Channel.findById(requestObject.getChannelCode());

            if (channel ==null || channel.equals(Channel.UNKNOWN))
            {
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.CHANNEL_NOT_FOUND,sessionId,requestObject.getChannelCode());
                return ResponseEntity.ok(otpResponse);
            }
            log.info("The Request is Coming through channel : "+channel);
            mandate = emandateBaseService.generateMRC(new Mandate(),subscriber, requestObject, mandateCode, product, biller,channel);
            if (mandate ==null)
            {
                otpResponse = OTPService.generateOTPResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,sessionId,"");
                return ResponseEntity.ok(otpResponse);
            }

            /**Cache mandate object in Redis and make a call to OTP service*/
            return emandateBaseService.saveToRedisCache(requestObject.getAmount(), sessionId, mandate);
//            EmandateResponse emandateResponse1 = makeMcashCallMRC(requestObject, mandate, subscriber, channel);
        } catch (CMMSException ex) {
            log.error("---Exception trace --- {} ", ex);
            otpResponse = new OTPResponse(ex.getMessage(),sessionId,ex.getEmandateErrorCode());
            return ResponseEntity.ok(otpResponse);
        }

    }
    public EmandateResponse makeMcashCallMRC(MRCMandateRequest requestObject, Mandate mandate, Subscriber subscriber, Channel channel) throws CMMSException {
        EmandateResponse emandateResponse = mcashAutheticationService.setupMcashRequest(mandate,"requestObject.getSubscriberPassCode()",channel.getValue());
        log.info("Mcash response : "+emandateResponse);
        if (StringUtils.isEmpty(emandateResponse.getResponseCode()))
            return  EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MCASH_NO_RESPONSE,null,subscriber.getBank().getName());
        if (!emandateResponse.getResponseCode().equalsIgnoreCase("00"))
            {
                emandateResponse.setMandateCode(null);
                return emandateResponse;
            }
            else return saveOperation(mandate);
    }
    public EmandateResponse saveOperation(Mandate mandate) throws CMMSException {
        log.info("Mandate to be Saved : "+mandate);
        mandate = save(mandate);
        if (mandate != null)
        {
            sendMandateAdvice.sendMandateAdvice(mandate);
            return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.CREATION_SUCCESSFUL,mandate.getMandateCode(),"");
        }
        else return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,mandate.getMandateCode(),"");
    }
    private Mandate save(Mandate mandate) throws CMMSException {
        try {
            return mandateRepo.save(mandate);
        }catch (Exception e)
        {
            log.error("--Exception trace --{} ",e);
            throw new CMMSException(EmandateResponseCode.UNKNOWN.getValue(),"500",EmandateResponseCode.UNKNOWN.getCode());
        }
    }

}
