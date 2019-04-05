package ng.upperlink.nibss.cmms.service.emandate.auth;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.config.cache.emandate.JWTEmandateOTP;
import ng.upperlink.nibss.cmms.config.cache.emandate.repository.RedisRepository;
import ng.upperlink.nibss.cmms.config.cache.emandate.repository.RedisRepositoryImpl;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RedisService {

    private static final String KEY = "userCmmsToken";
    @Autowired
    private JWTEmandateOTP jwtEmandateOTP;
    @Autowired
    private RedisRepository redisRepository;

    public <T>boolean saveObjToCache(T objct, String sessionId) {
        String token;
        token = jwtEmandateOTP.generateRequesToken(objct);
        redisRepository.add(token,sessionId);
        return true;
    }


//    public boolean setMadateAsLogged(String uniqueCode, String sessionId) {
//        hashOperations.put(KEY+uniqueCode, uniqueCode, sessionId);
//        redisTemplate.expire(KEY+uniqueCode, tokenTimeout, TimeUnit.DAYS);
//        return true;
//    }

//    public <T>String generateOTPToken(T object){
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = objectMapper.valueToTree(object);
//        //generateAuthRequest token
//        String token = Jwts.builder()
//                .setPayload(jsonNode.toString())
//                .signWith(SignatureAlgorithm.HS512, SecurityConstants.SECRET.getValue())
//                .compact();
//
//        return SecurityConstants.TOKEN_PREFIX.getValue()+" "+ token;
//    }


    public  String findTokenFromRedis(String sessionId) {
        return (String)redisRepository.findObj(sessionId);
    }
    public<T> T decodeToken(String token,boolean isMandate,Class<T> clazz) throws CMMSException {
        if (isMandate) return (T) jwtEmandateOTP.decodeMandateToken(token);
        else return jwtEmandateOTP.decodeTokenGeneric(token,clazz);
    }

    public void deleteTokenFromRedis(String sessionId) {
        redisRepository.delete(sessionId);
    }
    public static void main(String[] agrs)
    {
        String KEY = "EmandateToken";

        RedisTemplate<String, Object> redisTemplate;
        HashOperations hashOperations;
        RedisRepository redisRepository = new RedisRepositoryImpl();
        Object tokenFromRedis = redisRepository.findObj("eaf5a9f6-bdce-4447-959d-8ca25042e89e");
        System.out.println(tokenFromRedis);
        JWTEmandateOTP jwtEmandateOTP = new JWTEmandateOTP();
        try {
            Mandate mandate = jwtEmandateOTP.decodeMandateToken(tokenFromRedis.toString());
            System.out.println(mandate);
        } catch (CMMSException e) {
            e.printStackTrace();
        }
//        generateOPT("058","0231116887");
//        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
//                "<GenerateOTPRequest><MandateCode>0005400001</MandateCode ><TransType>2</TransType><BankCode> 023 </BankCode><BillerID>NIBSS0000000030</BillerID><BillerName>Upperlink</BillerName><Amount>10000</Amount ><BillerTransId>1045622</ BillerTransId ></GenerateOTPRequest>"+"67651F8E63889980F83AD46C3DB0A27B";
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<GenerateOTPRequest>\n" +
                "<AcctNumber>5050007512</ AcctNumber >\n" +
                "<AcctName>OKOLI CHUKWUMA PAUL</AcctName>\n" +
                "<TransType>2</TransType>\n" +
                "<BankCode> 070 </BankCode>\n" +
                "<BillerID>NIBSS0000000030</BillerID>\n" +
                "<BillerName>Upperlink</BillerName>\n" +
                "<Amount>500</Amount >\n" +
                "<BillerTransId>1045620</ BillerTransId > \n" +
                "</GenerateOTPRequest>"+"67651F8E63889980F83AD46C3DB0A27B";
        String sha256 = EncyptionUtil.doSHA256Encryption(xml);
        System.out.println(sha256);
    }
}
