package ng.upperlink.nibss.cmms.model.emandate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.model.SuperModel;
import ng.upperlink.nibss.cmms.model.User;
import ng.upperlink.nibss.cmms.model.authorization.AuthorizationTable;

import javax.persistence.*;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "EmandateConfiguration",schema = Constants.SCHEMA_NAME)
public class EmandateConfig extends AuthorizationTable implements Serializable{
    @Column
    private String username;

    @Column
    private String domainName;

    @Column(name = "notificationUrl")
    private String notificationUrl;

    @JsonIgnore
    @Column
    private String clientPassKey;

    @JsonIgnore
    @Column
    private String password;

    public EmandateConfig(Long id, String username, String domainName, String notificationUrl) {
        super(id);
        this.username = username;
        this.domainName = domainName;
        this.notificationUrl = notificationUrl;
    }
}
