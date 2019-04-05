package ng.upperlink.nibss.cmms.service.emandate;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.service.QueueService;
import ng.upperlink.nibss.cmms.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;

@Component
@Slf4j
public class SendMandateAdvice
    {

        @Value("${initiate.mandate.advice.topic}")
        private String mandatAdviceTopic;

        private QueueService queueService;

        @Autowired
        public void setQueueService(QueueService queueService) {
        this.queueService = queueService;
    }

        @Async
        public void sendMandateAdvice(Mandate mandate)
        {
            String mandateInString = CommonUtils.convertObjectToJson(mandate);
            queueService.send(mandatAdviceTopic,mandateInString);
        }
}
