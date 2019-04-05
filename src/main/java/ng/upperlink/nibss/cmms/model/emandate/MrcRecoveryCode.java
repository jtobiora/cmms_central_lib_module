package ng.upperlink.nibss.cmms.model.emandate;

import lombok.Data;
import lombok.NoArgsConstructor;
import ng.upperlink.nibss.cmms.enums.Constants;
import ng.upperlink.nibss.cmms.model.SuperModel;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(schema = Constants.SCHEMA_NAME)
public class MrcRecoveryCode extends SuperModel implements Serializable {
    @Id
    @GeneratedValue(strategy =  GenerationType.AUTO)
    @Column(name = "Id")
    private Long id;

    @JoinColumn
    @ManyToOne
    private Subscriber subscriber;

    @Basic(optional=false)
    @Column
    private String recoveryCode;

    @Column
    private boolean status = false;
}
