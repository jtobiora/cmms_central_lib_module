package ng.upperlink.nibss.cmms.service.emandate;

import ng.upperlink.nibss.cmms.dto.emandates.PaymentRequest;
import ng.upperlink.nibss.cmms.dto.emandates.PaymentStatusParam;
import ng.upperlink.nibss.cmms.dto.emandates.PaymentStatusRequest;
import ng.upperlink.nibss.cmms.dto.emandates.response.EmandateResponse;
import ng.upperlink.nibss.cmms.dto.emandates.response.PaymentResponse;
import ng.upperlink.nibss.cmms.dto.emandates.response.PaymentStatusResponse;
import ng.upperlink.nibss.cmms.enums.Channel;
import ng.upperlink.nibss.cmms.enums.TransactionStatus;
import ng.upperlink.nibss.cmms.enums.UserType;
import ng.upperlink.nibss.cmms.enums.emandate.EmandateResponseCode;
import ng.upperlink.nibss.cmms.enums.emandate.PaymentResponseCode;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.Transaction;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.service.QueueService;
import ng.upperlink.nibss.cmms.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private EmandateBaseService emandateBaseService;
    private TransactionService transactionService;
    private QueueService queueService;

    @Value("${initiate.mandate.transaction.topic}")
    private String paymentTopic;
    @Autowired
    public void setQueueService(QueueService queueService) {
        this.queueService = queueService;
    }

    @Autowired
    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Autowired
    public void setEmandateBaseService(EmandateBaseService emandateBaseService) {
        this.emandateBaseService = emandateBaseService;
    }

    public PaymentResponse initiatePayment(PaymentRequest request,UserType userType){
        try {
            switch (userType) {
                case BANK:
                    emandateBaseService.authenticateBank(request.getAuth());
                    break;
                case BILLER:
                    emandateBaseService.authenticateBiller(request.getAuth());
                    break;
                default:
                    throw new CMMSException("Request from unknown client", "404", EmandateResponseCode.UNAUTHORIZED.getCode());
            }
        }catch (CMMSException e) {
            e.printStackTrace();
            logger.error("Error track ----",e);
            return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),null,e.getEmandateErrorCode(),request.getSubscriberCode());
        }
        try
        {
            Mandate m = emandateBaseService.findByMandateCode(request.getMandateCode());
            if (m ==null)
            {
                return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),null,EmandateResponseCode.MANDATE_NOT_FOUND.getCode(),request.getSubscriberCode());
            }
            String narration = m.getNarration();
            logger.info("Mandate on which payment is initiated\n Code: "+m.getMandateCode()+"\n Amount: "+(m.isFixedAmountMandate() ? m.getAmount():m.getVariableAmount()));
            if (m.isFixedAmountMandate())
            {
                return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),narration,EmandateResponseCode.PAYMENT_NOT_ALLOWED.getCode(),request.getSubscriberCode());
            }
            if (request.getAmount().compareTo(m.getVariableAmount()) !=-1 && request.getAmount().compareTo(m.getVariableAmount()) !=0)
            {
                return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),narration,EmandateResponseCode.UNTHORIZED_AMOUNT.getCode(),request.getSubscriberCode());
            }
            if (!m.isMandateAdviceSent())
            {
                return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),narration,EmandateResponseCode.UNTHORIZED_MANDATE.getCode(),request.getSubscriberCode());
            }
//            m.setVariableAmount(request.getAmount());
            Transaction trans = transactionService.processTransaction(m, Channel.MOBILE_BANKING,request.getAmount());
                // send transction to queue
            if (trans ==null)
            {
                logger.error("Could not create transaction  for payment");
                return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),narration, PaymentResponseCode.UNKNOWN.getCode(),request.getSubscriberCode());
            }
            logger.info(trans.toString());
                /*Transaction trans = transaction;
                logger.info("Transactions created "+trans);
                if (trans ==null)
                {
                    return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),narration,EmandateResponseCode.PAYMENT_FAILED.getValue(),request.getSubscriberCode());
                }*/
                TransactionStatus transactionStatus =null;
                        try {
                            transactionStatus = queueService.processMandateTransaction(trans);
                        }catch (NoClassDefFoundError | Exception e)
                        {
                            logger.error("Error from quequService {}",e);
                            return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),narration,EmandateResponseCode.PAYMENT_FAILED.getCode(),request.getSubscriberCode());

                        }
                logger.info("Payment status of mandate with code: {}"+m.getMandateCode(),transactionStatus);
                switch (transactionStatus)
                {
                    case PAYMENT_FAILED:
                        return setPaymentResponse(trans, EmandateResponseCode.PAYMENT_FAILED.getCode(),request.getSubscriberCode());
                    case PAYMENT_REVERSED:
                        return setPaymentResponse(trans, EmandateResponseCode.PAYMENT_REVERSED.getCode(),request.getSubscriberCode());
                    case PAYMENT_SUCCESSFUL:
                        return setPaymentResponse(trans, EmandateResponseCode.PAYMENT_SUCCESSFUL.getCode(),request.getSubscriberCode());
                    case PAYMENT_IN_PROGRESS:
                        return setPaymentResponse(trans, EmandateResponseCode.PAYMENT_IN_PROGRESS.getCode(),request.getSubscriberCode());
                    case PAYMENT_ENTERED:
                        default:
                            return setPaymentResponse(trans, EmandateResponseCode.UNKNOWN.getCode(),request.getSubscriberCode());
                }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.toString());
            return new PaymentResponse(request.getAmount().toString(),request.getMandateCode(),null,EmandateResponseCode.PAYMENT_FAILED.getCode(),request.getSubscriberCode());
        }
    }
    public ResponseEntity<?> getPaymentStatus(PaymentStatusRequest request, UserType userType){
       try {
           ResponseEntity responseEntity = null;
           PaymentStatusResponse statusResponse = null;
           switch (userType) {
               case BANK:
                   emandateBaseService.authenticateBank(request.getAuth());
                   break;
               case BILLER:
                   emandateBaseService.authenticateBiller(request.getAuth());
                   break;
           }
       }catch (CMMSException e)
       {
           logger.error(e.getMessage(),e);
           e.printStackTrace();
           return ResponseEntity.ok(new EmandateResponse(e.getEmandateErrorCode(), request.getMandateCode(),e.getMessage()));
       }
       try
       {
           String mandateCode = request.getMandateCode();
           Mandate mandate = emandateBaseService.findByMandateCode(mandateCode);
           if (mandate == null) {
               logger.error("Mandate with code:" + mandateCode + " is not found");
               return ResponseEntity.ok(EmandateBaseService.generateEmandateResponse(EmandateResponseCode.MANDATE_NOT_FOUND, null,mandateCode));
           }
           /*
            List<Transaction> transList = new ArrayList<>()
            Optional<Transaction> transaction = transList.stream().max(Comparator.comparing(Transaction::getDateModified));
            */
           Transaction transaction = transactionService.getTransactionByMandateId(mandate.getId());
           if (transaction == null) {
               logger.error("Transaction of mandate with mandate Id :" + mandate.getId() + " is not found");
               return ResponseEntity.ok(EmandateBaseService.generateEmandateResponse(EmandateResponseCode.TRANSACTION_NOT_FOUND,mandateCode,mandateCode));
           }
           return ResponseEntity.ok(new PaymentStatusResponse(request.getMandateCode(), setParam(transaction)));
       }catch (Exception e)
       {
           logger.error(e.getMessage(),e);
           e.printStackTrace();
           return ResponseEntity.ok(new EmandateResponse(EmandateResponseCode.UNKNOWN.getCode(), request.getMandateCode(),EmandateResponseCode.UNKNOWN.getValue()));
       }

    }

    public PaymentStatusParam setParam(Transaction transaction) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String requestDate = f.format(transaction.getDateCreated());
//        String mandateCode = transaction.getMandate().getMandateCode();
        String valueDate = f.format(transaction.getPaymentDate());
        String numberOfTrials = String.valueOf(transaction.getNumberOfTrials());
        String numberOfCreditTrials = String.valueOf(transaction.getNumberOfCreditTrials());
        String status = transaction.getStatus().name();

        return new PaymentStatusParam(requestDate,valueDate,numberOfTrials,numberOfCreditTrials,status);
    }
    public PaymentResponse setPaymentResponse(Transaction transaction, String responseCode,String subscriberCode)
    {
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setAmount(transaction.getAmount().toString());
//        paymentResponse.setBatchId(null);
        paymentResponse.setMandateCode(transaction.getMandate().getMandateCode());
        paymentResponse.setNarration(transaction.getMandate().getNarration());
        paymentResponse.setResponseCode(responseCode);
        paymentResponse.setSubscriberCode(subscriberCode);
        return paymentResponse;
    }

}

