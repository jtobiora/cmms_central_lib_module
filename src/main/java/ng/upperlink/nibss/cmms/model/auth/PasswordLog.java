/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ng.upperlink.nibss.cmms.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import javax.persistence.*;

import ng.upperlink.nibss.cmms.enums.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ng.upperlink.nibss.cmms.model.SuperModel;
import ng.upperlink.nibss.cmms.model.User;


@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@ToString
@Table(name="PasswordLog",schema = Constants.SCHEMA_NAME)
public class PasswordLog extends SuperModel implements Serializable{
    
    @Id
    @GeneratedValue(strategy =  GenerationType.AUTO)
    @Column(name = "Id")
    private Long id;
    
    @Basic(optional = false)
    @JoinColumn(name="UserId", referencedColumnName = "Id")
    @ManyToOne(optional = false)
    private User user;
    
    @Basic(optional=false)
    @Column(name ="Password")
    private String password;
}
