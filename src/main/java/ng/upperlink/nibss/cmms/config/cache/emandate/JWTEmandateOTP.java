package ng.upperlink.nibss.cmms.config.cache.emandate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import ng.upperlink.nibss.cmms.config.cache.JWTRedisToken;
import ng.upperlink.nibss.cmms.config.cache.UserLoginCacheService;
import ng.upperlink.nibss.cmms.config.cache.UserTokenCacheService;
import ng.upperlink.nibss.cmms.dto.emandates.AccessToken;
import ng.upperlink.nibss.cmms.dto.emandates.BankDetails;
import ng.upperlink.nibss.cmms.dto.emandates.BillerDetails;
import ng.upperlink.nibss.cmms.enums.SecurityConstants;
import ng.upperlink.nibss.cmms.enums.emandate.EmandateResponseCode;
import ng.upperlink.nibss.cmms.exceptions.CMMSException;
import ng.upperlink.nibss.cmms.model.emandate.Subscriber;
import ng.upperlink.nibss.cmms.model.mandate.Mandate;
import ng.upperlink.nibss.cmms.service.bank.BankService;
import ng.upperlink.nibss.cmms.service.biller.BillerService;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

@Service
public class JWTEmandateOTP{

    private Logger logger = LoggerFactory.getLogger(JWTEmandateOTP.class);
    @Value("${encryption.salt}")
    private String salt;
    private BankService bankService;
    private BillerService billerService;
    private JWTRedisToken jwtRedisToken;
    private UserLoginCacheService userLoginCacheService;
    private UserTokenCacheService userTokenCacheService;

    @Autowired
    public void setUserTokenCacheService(UserTokenCacheService userTokenCacheService)
    {
        this.userTokenCacheService = userTokenCacheService;
    }
    @Autowired
    public void setUserLoginCacheService(UserLoginCacheService userLoginCacheService) {
        this.userLoginCacheService = userLoginCacheService;
    }

    @Autowired
    public void setJwtRedisToken(JWTRedisToken jwtRedisToken) {
        this.jwtRedisToken = jwtRedisToken;
    }

    @Autowired
    public void setBillerService(BillerService billerService) {
        this.billerService = billerService;
    }

    @Autowired
    public void setBankService(BankService bankService) {
        this.bankService = bankService;
    }

    /**This generates the token to be cached in redis*/
    public <T> String generateRequesToken(T obj) {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.valueToTree(obj);
        String token = Jwts.builder()
                .setPayload(jsonNode.toString())
                .signWith(SignatureAlgorithm.HS512,  SecurityConstants.SECRET.getValue())
                .compact();
        return token;
    }

    public Mandate decodeMandateToken(String encodedToken) throws CMMSException {
        try {
            Claims claimsString = claims(encodedToken);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd");
            Date startDate = null;
            Date endDate = null;
            Date nextDebitDate = null;

//            startDate = simpleDateFormat.parse(claimsString.get("startDate").toString());
//            endDate = simpleDateFormat.parse(claimsString.get("endDate").toString());
//            nextDebitDate = simpleDateFormat.parse(claimsString.get("nextDebitDate").toString());
//
//            claimsString.put("startDate", startDate);
//            claimsString.put("endDate", endDate);
//            claimsString.put("nextDebitDate", nextDebitDate);

            Mandate mandate = new ObjectMapper().convertValue(claimsString, Mandate.class);
            mandate.setDateAuthorized(new Date());
            mandate.setDateAccepted(new Date());
            mandate.setDateApproved(new Date());
            return mandate;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception {} ",e);
            throw new CMMSException(EmandateResponseCode.UNKNOWN.getValue(),"500",EmandateResponseCode.UNKNOWN.getCode());
        }

    }

    public<T> T decodeTokenGeneric(String encodedToken, Class<T> clzz) throws CMMSException {
       try {
           if (encodedToken != null) {
               Claims claims = claims(encodedToken);
               if (claims == null) {
                   logger.error("Unable to retrieve Claims");
                   return null;
               }
               T t = new ObjectMapper().convertValue(claims, clzz);
//            subscriber.setMrcPin(null);
//            subscriber.setMrc(null);
//            subscriber.setActivated(true);
//            subscriber.setAccountName(null);
//            subscriber.setAccountNumber(null);
//            subscriber.setBank(null);
//            subscriber.setBvn(null);
//            subscriber.setChannel(null);
//            subscriber.setEmail(null);
//            subscriber.setPayerName(null);
//            subscriber.setPayerAddress(null);
//            subscriber.setPhoneNumber(null);

               return t;

           } else {
               logger.error("passed empty header objects");
               return null;
           }
       }catch (Exception e) {
           e.printStackTrace();
           logger.error("Exception {} ",e);
           throw new CMMSException(EmandateResponseCode.UNKNOWN.getValue(),"500",EmandateResponseCode.UNKNOWN.getCode());
       }
    }

    public Claims claims(String encodedToken) {
        if (encodedToken != null) {
            // parse the token.
            Claims claims = null;
            try {
                claims = Jwts.parser()
                        .setSigningKey(SecurityConstants.SECRET.getValue())
                        .parseClaimsJws(encodedToken.trim())
                        .getBody();
            } catch (ExpiredJwtException e) {
                logger.error("Unable to parse token: ExpiredJwtException ", e);
                return null;
            } catch (UnsupportedJwtException e) {
                logger.error("Unable to parse token : UnsupportedJwtException", e);
                return null;
            } catch (MalformedJwtException e) {
                logger.error("Unable to parse token :MalformedJwtException", e);
                return null;
            } catch (SignatureException e) {
                logger.error("Unable to parse token: SignatureException", e);
                return null;
            } catch (IllegalArgumentException e) {
                logger.error("Unable to parse token : IllegalArgumentException", e);
                return null;
            } catch (Exception e) {
                logger.error("Unable to parse token :Exception", e);
                return null;
            }
            if (claims == null) {
                logger.error("Unable to retrieve Claims");
                return null;
            }
            return claims;
        } else {
            logger.error("passed empty header objects");
            return null;
        }

    }

    public boolean sha512Valid(String sha512TokenEncrypted, String sha512RequestToken) {

        if (sha512TokenEncrypted.equals(sha512RequestToken))
            return true;
        else
            return false;
    }

    public String generateSha512(BillerDetails billerDetails) {
        String sha512Token = billerDetails.getApiKey() + billerDetails.getAccountNumber() + billerDetails.getRcNumber();
        String sha512TokenEncrypted = EncyptionUtil.doSHA512Encryption(sha512Token, billerDetails.getBillerPassKey());
        if (sha512TokenEncrypted.isEmpty())
            return null;
        return sha512TokenEncrypted;
    }

    public String generateSha512(BankDetails bankDetails)
    {
        String sha512Token = bankDetails.getApiKey()+ bankDetails.getCode()+bankDetails.getNipCode();
        String sha512TokenEncrypted = EncyptionUtil.doSHA512Encryption(sha512Token,bankDetails.getSecretKey());
        if (sha512TokenEncrypted.isEmpty())
            return null;
        return sha512TokenEncrypted;
    }

    public String generateBase64(String code) {
        String base64encodedString = null;
        // Encode using basic encoder
        try {

            base64encodedString = Base64.getEncoder().encodeToString(
                    code.getBytes("utf-8"));
            return null;
        } catch (UnsupportedEncodingException e) {
            return base64encodedString;
        }
    }

    public String decodeBase64(String base64String) {

        try {
            // Decode
            byte[] base64decodedBytes = Base64.getDecoder().decode(base64String);
            String code = new String(base64decodedBytes, "utf-8");
            return code;
        } catch (UnsupportedEncodingException e) {
            return "Error occured " + e.getMessage();
        }
    }

//    public boolean isValidMandateSession(String sessionId) {
//        if (sessionId == null)
//            System.out.printf("The session is null");
//        return jwtRedisToken.isValidUserSession(sessionId);
//    }

    public String findSession(String access) {
        return userLoginCacheService.findSession(access);
    }

    /*
    public String generateToken(AccessToken accessToken, String secreteKey) {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.valueToTree(accessToken);
        String token = Jwts.builder()
                .setPayload(jsonNode.toString())
                .signWith(SignatureAlgorithm.HS512, secreteKey)
                .compact();
        return token;
    }*/
}
