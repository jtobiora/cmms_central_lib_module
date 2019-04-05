package ng.upperlink.nibss.cmms.service.emandate;

import ng.upperlink.nibss.cmms.dto.emandates.EmandateRequest;
import ng.upperlink.nibss.cmms.dto.emandates.MRCMandateRequest;
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
import ng.upperlink.nibss.cmms.util.MandateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BankEmandateService
{

    @Value("${encryption.salt}")
    private String salt;
    private static Logger logger = LoggerFactory.getLogger(BankEmandateService.class);
    private ProductService productService;
    private BankService bankService;
    private MandateRepo mandateRepo ;
    private EmandateBaseService emandateBaseService;
    private SubscriberService subscriberService;
    private SendMandateAdvice sendMandateAdvice;

    @Autowired
    public void setSubscriberService(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @Autowired
    public void setSendMandateAdvice(SendMandateAdvice sendMandateAdvice) {
        this.sendMandateAdvice = sendMandateAdvice;
    }

    @Autowired
    public void setEmandateBaseService(EmandateBaseService emandateBaseService) {
        this.emandateBaseService = emandateBaseService;
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
    public EmandateResponse processMandateWithoutMRC(EmandateRequest requestObject) throws CMMSException {

        Mandate mandate = new Mandate();
        Product product =null;
        Biller biller =null;
        String rcNumber =null;
        Bank subcriberBank =null;
        String mandateCode =null;
        Channel channel = null;

        try {
            product = productService.getProductById(requestObject.getProductId());
            if (product==null)
            {
                return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.PRODUCT_NOT_FOUND, null,String.valueOf(requestObject.getProductId()));
            }
            biller = product.getBiller();
            if (biller==null){
                return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.UNKNOWN,null,String.valueOf(requestObject.getProductId()));
            }
            rcNumber = biller.getRcNumber();

            //get the subscriber's bank
            subcriberBank = bankService.getBankByCode(requestObject.getBankCode());

            if (subcriberBank == null){
                return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.SUBSCRIBER_BANK_NOT_FOUND,null,requestObject.getBankCode());
            }
            mandateCode = MandateUtils.getMandateCode(String.valueOf(System.currentTimeMillis()), rcNumber, String.valueOf(product.getId()));
            if (mandateCode ==null)
                return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,null,"");
            logger.info("Generated mandate code "+mandateCode );

                channel = Channel.findById(requestObject.getChannelCode());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                throw new CMMSException(EmandateResponseCode.CHANNEL_NOT_FOUND.getValue().replace("{}",requestObject.getChannelCode()),"400",EmandateResponseCode.CHANNEL_NOT_FOUND.getCode());
            }
            if (channel ==null || channel.equals(Channel.UNKNOWN))
                return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.CHANNEL_NOT_FOUND,null,requestObject.getChannelCode());
            logger.info("The Request is Coming through channel : "+channel);
            //generateAuthRequest mandate
            mandate = emandateBaseService.generateMandate(mandate, requestObject, mandateCode, product, subcriberBank,biller,channel);
            logger.info("Mandate to be saved "+mandate);


            if (mandate ==null)
            {
                return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,mandate.getMandateCode(),"");

            }
        mandate.setDateAccepted(new Date());
        mandate.setDateApproved(new Date());
        mandate.setDateAuthorized(new Date());
        mandate.setDateCreated(new Date());
        return saveMandate(mandate);

    }
    public ResponseEntity<?> processMandateWithMRC(MRCMandateRequest requestObject, String sessionId) throws CMMSException {

        Mandate mandate = new Mandate();
        Product product =null;
        Biller biller =null;
        String rcNumber =null;
        String mandateCode =null;
        Channel channel = null;
        Subscriber subscriber =null;
        EmandateResponse emandateResponse;

        try {
            product = productService.getProductById(requestObject.getProductId());
            if (product==null)
            {
                emandateResponse= EmandateBaseService.generateEmandateResponse(EmandateResponseCode.PRODUCT_NOT_FOUND, null,String.valueOf(requestObject.getProductId()));
                return ResponseEntity.ok(emandateResponse);
            }
            biller = product.getBiller();
            if (biller==null){
                 emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.UNKNOWN, null, String.valueOf(requestObject.getProductId()));
                return ResponseEntity.ok(emandateResponse);
            }
            rcNumber = biller.getRcNumber();

            /**
             * Get the subscriber using the MRC */
            subscriber = subscriberService.getByCode(requestObject.getMandateReferenceCode());
            if (subscriber ==null) {
                emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.INVALID_MRC, null, String.valueOf(requestObject.getMandateReferenceCode()));
                return ResponseEntity.ok(emandateResponse);
            }
            emandateResponse = emandateBaseService.authenticateSubscriber(requestObject, subscriber);
            if (emandateResponse != null) return ResponseEntity.ok(emandateResponse);

                mandateCode = MandateUtils.getMandateCode(String.valueOf(System.currentTimeMillis()), rcNumber, String.valueOf(product.getId()));
            if (mandateCode ==null) {
                emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED, null, "");
                return ResponseEntity.ok(emandateResponse);
            }
            logger.info("Generated mandate code "+mandateCode );

                channel = Channel.findById(requestObject.getChannelCode());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                throw new CMMSException(EmandateResponseCode.CHANNEL_NOT_FOUND.getValue().replace("{}",requestObject.getChannelCode()),"400",EmandateResponseCode.CHANNEL_NOT_FOUND.getCode());
            }
            if (channel ==null || channel.equals(Channel.UNKNOWN)) {
                emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.CHANNEL_NOT_FOUND, null, requestObject.getChannelCode());
                return ResponseEntity.ok(emandateResponse);
            }
            logger.info("The Request is Coming through channel : "+channel);
            //generateAuthRequest mandate
            mandate = emandateBaseService.generateMRC(mandate, subscriber, requestObject, mandateCode, product,biller,channel);

        if (mandate ==null)
        {
            emandateResponse = EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED, mandate.getMandateCode(), "");
            return ResponseEntity.ok(emandateResponse);

        }

//        mandate.setDateAccepted(new Date());
//        mandate.setDateApproved(new Date());
//        mandate.setDateAuthorized(new Date());
//        mandate.setDateCreated(new Date());
//        logger.info("Mandate to be saved "+mandate);
            return emandateBaseService.saveToRedisCache(requestObject.getAmount(),sessionId,mandate);


    }

    public EmandateResponse saveMandate(Mandate mandate) throws CMMSException {
        try {
            mandate = mandateRepo.save(mandate);
            if (mandate ==null)
            {
                return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MANDATE_NOT_GENERATEED,mandate.getMandateCode(),"");

            }
            sendMandateAdvice.sendMandateAdvice(mandate);
            return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.CREATION_SUCCESSFUL,mandate.getMandateCode(),"");

        }catch (Exception e)
        {
            logger.error("--Exception trace --{} ",e);
            throw new CMMSException(EmandateResponseCode.UNKNOWN.getValue(),"500",EmandateResponseCode.UNKNOWN.getCode());
        }
    }


}
