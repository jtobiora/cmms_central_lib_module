package ng.upperlink.nibss.cmms.util.emandate.soap.bank;

import lombok.extern.slf4j.Slf4j;
import ng.upperlink.nibss.cmms.dto.bank.AccountLookUpRequest;
import ng.upperlink.nibss.cmms.dto.emandates.AuthenticationRequest;
import ng.upperlink.nibss.cmms.dto.emandates.otp.GenerateOTPRequest;
import ng.upperlink.nibss.cmms.dto.emandates.otp.ValidateOTPRequest;
import ng.upperlink.nibss.cmms.dto.emandates.response.AuthenticationResponse;
import ng.upperlink.nibss.cmms.service.emandate.auth.OTPService;
import ng.upperlink.nibss.cmms.util.emandate.soap.builder.SoapBuilder;
import ng.upperlink.nibss.cmms.util.emandate.soap.builder.SoapPayload;
import ng.upperlink.nibss.cmms.util.encryption.EncyptionUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

@Slf4j
public class XMLBuilder {
    public static String getNameEnquireRequestXML(AccountLookUpRequest request) {

        try{
            SoapBuilder soapBuilder = new SoapBuilder("");
            soapBuilder.setAsXml();
            soapBuilder.set("AccountLookUpRequest", new SoapPayload());
            soapBuilder.get("AccountLookUpRequest").set("Header", new SoapPayload());
            soapBuilder.get("AccountLookUpRequest").get("Header").set("EnquiryId", new SoapPayload(request.getEnquiryId()));
            soapBuilder.get("AccountLookUpRequest").get("Header").set("ClientId", new SoapPayload(request.getClientId()));
            soapBuilder.get("AccountLookUpRequest").get("Header").set("Salt", new SoapPayload(request.getSalt()));
            soapBuilder.get("AccountLookUpRequest").get("Header").set("Mac", new SoapPayload(request.getMac()));
            soapBuilder.get("AccountLookUpRequest").set("AccountRecord", new SoapPayload());
            soapBuilder.get("AccountLookUpRequest").get("AccountRecord").set("BankCode", new SoapPayload(request.getBankCode()));
            soapBuilder.get("AccountLookUpRequest").get("AccountRecord").set("AccountNumber", new SoapPayload(request.getAccountNumber()));

            return soapBuilder.getXml();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static SoapBuilder generateOTPRequestXML(GenerateOTPRequest request) {

        try{
            SoapBuilder soapBuilder = new SoapBuilder("");
            soapBuilder.setAsXml();
            soapBuilder.set("GenerateOTPRequest", new SoapPayload());
            soapBuilder.get("GenerateOTPRequest").set("MandateCode", new SoapPayload(request.getMandateCode()));
            soapBuilder.get("GenerateOTPRequest").set("TransType",new SoapPayload(request.getTransType()));
            soapBuilder.get("GenerateOTPRequest").set("BankCode",new SoapPayload(request.getBankCode()));
            soapBuilder.get("GenerateOTPRequest").set("BillerID",new SoapPayload(request.getBillerId()));
            soapBuilder.get("GenerateOTPRequest").set("BillerName",new SoapPayload(request.getBillerName()));
            soapBuilder.get("GenerateOTPRequest").set("Amount", new SoapPayload(request.getAmount()));
            soapBuilder.get("GenerateOTPRequest").set("BillerTransId",new SoapPayload(request.getBillerTransOd()));
            String xml = soapBuilder.getXml();
//            System.out.println("XML Built \n"+xml);
            soapBuilder.get("GenerateOTPRequest").set("HashValue",new SoapPayload(EncyptionUtil.doSHA256Encryption(xml+ OTPService.getKEY())));
//            xml = soapBuilder.getXml();
//            System.out.println("XML Built with hash\n"+xml);
            return soapBuilder;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static SoapBuilder generateValidateOTPRequestXML(ValidateOTPRequest request) {

        try{
            SoapBuilder soapBuilder = new SoapBuilder("");
            soapBuilder.setAsXml();
            soapBuilder.set("ValidateOTPRequest", new SoapPayload());
            soapBuilder.get("ValidateOTPRequest").set("MandateCode", new SoapPayload(request.getMandateCode()));
            soapBuilder.get("ValidateOTPRequest").set("TransType",new SoapPayload(request.getTransType()));
            soapBuilder.get("ValidateOTPRequest").set("BankCode",new SoapPayload(request.getBankCode()));
            soapBuilder.get("ValidateOTPRequest").set("OTP",new SoapPayload(request.getOtp()));
            soapBuilder.get("ValidateOTPRequest").set("BillerID",new SoapPayload(request.getBillerId()));
            soapBuilder.get("ValidateOTPRequest").set("BillerName",new SoapPayload(request.getBillerName()));
            soapBuilder.get("ValidateOTPRequest").set("Amount", new SoapPayload(request.getAmount()));
            soapBuilder.get("ValidateOTPRequest").set("BillerTransId",new SoapPayload(request.getBillerTransOd()));
            String xml = soapBuilder.getXml();
//            System.out.println("XML Built \n"+xml);
            soapBuilder.get("ValidateOTPRequest").set("HashValue",new SoapPayload(EncyptionUtil.doSHA256Encryption(xml+ OTPService.getKEY())));
            xml = soapBuilder.getXml();
//            System.out.println("XML Built with hash\n"+xml);
            return soapBuilder;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String mcashAuthRequetXML(AuthenticationRequest request) {

        try{
            SoapBuilder soapBuilder = new SoapBuilder("");
            soapBuilder.setAsXml();
            soapBuilder.set("AuthenticationRequest", new SoapPayload());
            soapBuilder.get("AuthenticationRequest").set("SessionID",new SoapPayload(request.getSessionId()));
            soapBuilder.get("AuthenticationRequest").set("RequestorID",new SoapPayload(request.getRequestorID()));
            soapBuilder.get("AuthenticationRequest").set("PayerPhoneNumber",new SoapPayload(request.getPayerPhoneNumber()));
            soapBuilder.get("AuthenticationRequest").set("MandateReferenceNumber", new SoapPayload(request.getMandateReferenceNumber()));
            soapBuilder.get("AuthenticationRequest").set("ProductCode", new SoapPayload(request.getProductCode()));
            soapBuilder.get("AuthenticationRequest").set("Amount", new SoapPayload(request.getAmount()));
            soapBuilder.get("AuthenticationRequest").set("AdditionalFIRequiredData", new SoapPayload(request.getAdditionalFIRequiredData()));
            soapBuilder.get("AuthenticationRequest").set("FIInstitution", new SoapPayload(request.getFIInstitution()));
            soapBuilder.get("AuthenticationRequest").set("AccountNumber", new SoapPayload(request.getAccountNumber()));
            soapBuilder.get("AuthenticationRequest").set("AccountName", new SoapPayload(request.getAccountName()));
            soapBuilder.get("AuthenticationRequest").set("PassCode", new SoapPayload(request.getPassCode()));

            return soapBuilder.getXml();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static   <T> String marshal(T object, Class<T> clazz) {
        StringWriter writer = new StringWriter();

        try{
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            jaxbMarshaller.marshal(object, writer);
            String xmlResponse = writer.toString();
            return xmlResponse;
        } catch(JAXBException ex) {
            log.error("Marshaller Exception: ", ex);
            ex.printStackTrace();
            return null;
        }
    }
    public static void xmlToObject(String xml)
    {
        JAXBContext jaxbContext;
        try
        {
            jaxbContext = JAXBContext.newInstance(AuthenticationResponse.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            AuthenticationResponse response = (AuthenticationResponse) jaxbUnmarshaller.unmarshal(new StringReader(xml));

            System.out.println(response);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }
    }

    public static <T> T unmarshalWithoutDecrypting(String object, Class<T> clazz) {
        StringReader reader = new StringReader(object);

        try{
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return clazz.cast(jaxbUnmarshaller.unmarshal(reader));
        } catch(JAXBException ex) {
            return null;
        }
    }
    public static void main (String[]args)
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                " <AuthenticationResponse> " +
                " <SessionID>999999700101010000CAC010045820</SessionID> " +
                " <RequestorID>999058</RequestorID> " +
                " <PayerPhoneNumber>08062864121</PayerPhoneNumber>  " +
                " <MandateReferenceNumber>CAC01/004/58200837</MandateReferenceNumber> " +
                " <ProductCode>USSD</ProductCode>  " +
                " <Amount>100.0</Amount> " +
                " <FIInstitution>999058</FIInstitution> " +
                " <AccountNumber>0231116887</AccountNumber>  " +
                " <AccountName>Odinaka Henry Onah</AccountName> " +
                " <ResponseCode>63</ResponseCode> " +
                " </AuthenticationResponse>";
//        xmlToObject(xml);
        AuthenticationResponse authenticationResponse = unmarshalWithoutDecrypting(xml, AuthenticationResponse.class);
        System.out.println(authenticationResponse);

//        AuthenticationRequest authenticationRequest =new AuthenticationRequest("398498","req","98939804","ouiuhbf","jdkjkjkf",new BigDecimal(900),"jfkjhfjf","jfhjfjjfhf","jhdjajfjha","jkkdjjdkf","jdjdkjd");
//        String xml = mcashAuthRequetXML(authenticationRequest);
//        String xml = marshal(authenticationRequest);
//        System.out.println(xml);
    }
}
