package ng.upperlink.nibss.cmms.model.emandate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ng.upperlink.nibss.cmms.embeddables.makerchecker.MakerChecker;
import ng.upperlink.nibss.cmms.enums.Channel;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.enums.MakerCheckerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ng.upperlink.nibss.cmms.model.SuperModel;
import ng.upperlink.nibss.cmms.model.bank.Bank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;


@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "Subscriber",schema = Constants.SCHEMA_NAME)
public class Subscriber extends SuperModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "Id")
    private Long id;

    @NotNull
    @Column(unique = true, name = "MandateReferenceCode")
    private String mrc;

    private String email;

    private String payerName;

    private String payerAddress;

    @ManyToOne
    @JoinColumn(  referencedColumnName ="Id" )
    private Bank bank ;

    @NotNull(message = "Account Name can not be Blank")
    private String accountName;

    @Column(name = "BankAccountNumber")
    private String accountNumber;

    private String phoneNumber;

    private String bvn;

    @Column(name = "Activated")
    private boolean activated;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(name = "MRCPin")
    @JsonIgnore
    private String mrcPin;

    public Subscriber(Long id ,String mrc, String email, String payerName, Bank bank) {
        this.id =id;
        this.mrc = mrc;
        this.email = email;
        this.payerName = payerName;
        this.bank = bank;
    }
    public Subscriber(Long id ,String mrc, String email) {
        this.id =id;
        this.mrc = mrc;
        this.email = email;
    }
}
