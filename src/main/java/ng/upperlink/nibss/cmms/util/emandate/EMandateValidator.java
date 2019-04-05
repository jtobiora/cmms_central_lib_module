package ng.upperlink.nibss.cmms.util.emandate;

import com.fasterxml.jackson.core.JsonProcessingException;
import ng.upperlink.nibss.cmms.dto.account.request.AccountRequest;
import ng.upperlink.nibss.cmms.dto.account.response.AccountResponse;
import ng.upperlink.nibss.cmms.dto.emandates.EmandateRequest;
import ng.upperlink.nibss.cmms.dto.emandates.MRCMandateRequest;
import ng.upperlink.nibss.cmms.dto.emandates.response.EmandateResponse;
import ng.upperlink.nibss.cmms.enums.Channel;
import ng.upperlink.nibss.cmms.enums.emandate.EmandateResponseCode;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.service.account.AccountValidationService;
import ng.upperlink.nibss.cmms.service.account.ICADService;
import ng.upperlink.nibss.cmms.service.emandate.EmandateBaseService;
import ng.upperlink.nibss.cmms.util.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class EMandateValidator {
    EmandateFormValidation emandateFormValidation;

    @Autowired
    public void setEmandateFormValidation(EmandateFormValidation emandateFormValidation) {
        this.emandateFormValidation = emandateFormValidation;
    }

    private static final Logger logger = LoggerFactory.getLogger(EMandateValidator.class);

    public EmandateResponse validate(Object target) throws CMMSException {
        logger.info("Doing validation...");
        EmandateRequest mandateReq = null;
        MRCMandateRequest mrcMandateRequest= null;

        Integer frequency = null;
        String startDate1 = null;
        boolean fixedAmountMandate =false;
        String endDate1 = null;

        if (target instanceof EmandateRequest)
        {
            mandateReq =(EmandateRequest) target;
            fixedAmountMandate = mandateReq.isFixedAmountMandate();
            frequency = mandateReq.getFrequency();
            startDate1 = mandateReq.getStartDate();
            endDate1 = mandateReq.getEndDate();
        }
        if (target instanceof MRCMandateRequest)
        {
            mrcMandateRequest =(MRCMandateRequest) target;
            fixedAmountMandate = mrcMandateRequest.isFixedAmountMandate();
            frequency = mrcMandateRequest.getFrequency();
            startDate1 = mrcMandateRequest.getStartDate();
            endDate1 = mrcMandateRequest.getEndDate();
        }

        logger.info("mandate.getFrequency() "+ frequency);
        logger.info("mandate.getMandateStartDate() "+ startDate1);
        if (fixedAmountMandate && frequency <=0)
        {
            return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.INVALID_FREQUENCY,null,"");
        }
        if(frequency > 0 && (!startDate1.equals("") || !endDate1.equals(""))){
            logger.info("Trying to validate period and frequency");
            String sDate = startDate1;
            String eDate = endDate1;

            SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = null ;
            Date endDate = null;

            try {
                startDate = sdf.parse(sDate);
                endDate = sdf.parse(eDate);

               // Tue Nov 09 00:00:00 WAT 34
               // Mon Jan 28 19:37:46 WAT 2019

                if(startDate.compareTo(DateUtils.nullifyTime(new Date())) <= 0){

                    return EmandateBaseService.generateEmandateResponse(EmandateResponseCode.INVALID_START_DATE,null,sDate);

                } else {
                    long difference = endDate.getTime() - startDate.getTime();

                    difference = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS);

                    logger.info("Date difference between start and end date is " + difference);

                    if(difference < (frequency * 7)){
                        return new EmandateResponse(EmandateResponseCode.INVALID_DATE_RANGE.getCode(),null,EmandateResponseCode.INVALID_DATE_RANGE.getValue());

                    }

                }
                if (validateForm(target) !=null)
                    return validateForm(target);
            } catch (ParseException e) {
                String dateWithError = null;
                if (startDate ==null)
                    dateWithError = startDate1;
                else
                    dateWithError = endDate1;
                logger.error(e.getMessage(),e);
                return new EmandateResponse(EmandateResponseCode.PARSE_DATE_ERROR.getCode(),null,EmandateResponseCode.PARSE_DATE_ERROR.getValue().replace("{}",Optional.ofNullable(dateWithError).orElse("null")));

            }
        }
        return null;
    }
    public static AccountResponse generateAccountName(EmandateRequest request) throws CMMSException{
        AccountResponse response = null;
        response = AccountValidationService.request(new AccountRequest(request.getBankCode(),request.getAccountNumber()));
        Optional<String> accName = Optional.ofNullable(response.getAccountName());
        if (!accName.isPresent())
        {
            throw new CMMSException("Account number is not valid","400","400");
        }
        return response;
    }
//    public EmandateResponse validateForm(EmandateRequest request)
//    {
//
//        String e1 = emandateFormValidation.validateBankInformation(request.getAccountNumber(), request.getBankCode());
//        String e2 = emandateFormValidation.validateContactDetails(request.getPhoneNumber(), request.getEmailAddress(), request.getPayerName(), request.getPayerAddress());
//        String e3 = emandateFormValidation.validateRate(request.isFixedAmountMandate(), request.getAmount(), request.getFrequency(),request.getNarration(), request.getProductId());
//        String e4 = emandateFormValidation.validateRequestCodes(request.getChannelCode(), request.getSubscriberCode(), request.getSubscriberPassCode(), request.isFixedAmountMandate());
//        List<String> errors = Arrays.asList(e1,e2,e3,e4);
//        if (errors.isEmpty())
//            return null;
//        else
//            return new EmandateResponse(EmandateResponseCode.INVALID_REQUEST.getCode(),null,errors.toString());
//    }

    public EmandateResponse validateForm(Object obj)
    {
        List<String> errors = new ArrayList<>();
        EmandateRequest request =null;
        MRCMandateRequest mrcMandateRequest =null;
        Optional<String> e1 = null;
        Optional<String> e2 =null;
        Optional<String> e3 = null;
        Optional<String> e4 =null;
        if (obj instanceof EmandateRequest) {
            request = (EmandateRequest) obj;
            e1 = Optional.ofNullable(emandateFormValidation.validateBankInformation(request.getAccountNumber(), request.getBankCode()));
            e2 = Optional.ofNullable(emandateFormValidation.validateContactDetails(request.getPhoneNumber(), request.getEmailAddress(), request.getPayerName(), request.getPayerAddress()));
            e3 = Optional.ofNullable(emandateFormValidation.validateRate(request.isFixedAmountMandate(), request.getAmount(), request.getFrequency(), request.getNarration(), request.getProductId()));
            e4 = Optional.ofNullable(emandateFormValidation.validateRequestCodes(request.getChannelCode(), request.getSubscriberCode(), "1234", request.isFixedAmountMandate()));
            if (e3.isPresent()) {
                errors.add(e3.get());
            }
            if (e4.isPresent()) {
                errors.add(e4.get());
            }
        }
        if (obj instanceof MRCMandateRequest)
        {
             mrcMandateRequest = (MRCMandateRequest)obj;
            e1 = Optional.ofNullable(emandateFormValidation.validateRate(mrcMandateRequest.isFixedAmountMandate(), mrcMandateRequest.getAmount(), mrcMandateRequest.getFrequency(), mrcMandateRequest.getNarration(), mrcMandateRequest.getProductId()));
            e2 = Optional.ofNullable(emandateFormValidation.validateRequestCodes(mrcMandateRequest.getChannelCode(), mrcMandateRequest.getSubscriberCode(), "1234", mrcMandateRequest.isFixedAmountMandate()));
        }


        if (e1.isPresent()) {
            errors.add(e1.get());
        }
        if (e2.isPresent()) {
            errors.add(e2.get());
        }
        if (errors.isEmpty())
            return null;
        else
            return new EmandateResponse(EmandateResponseCode.INVALID_REQUEST.getCode(),null,errors.toString());
    }
}
