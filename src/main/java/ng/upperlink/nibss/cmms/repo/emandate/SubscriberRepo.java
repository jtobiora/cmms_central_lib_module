package ng.upperlink.nibss.cmms.repo.emandate;

import ng.upperlink.nibss.cmms.model.emandate.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;


@Transactional
@Repository
public interface SubscriberRepo extends JpaRepository<Subscriber, Long>{

//    String BASE_DTO_QUERY = "select new ng.upperlink.nibss.cmms.dto.SubscriberDto(s.id,u.name.firstName,u.name.lastName,u.contactDetails.lga.state.name," +
//            "u.contactDetails.lga.name,u.emailAddress,u.bvn,s.code, s.createdAt) from Subscriber s join s.user u";
//    //MAKER CHECKER
//    String BASE_ALL_JSON_QUERY = "select new ng.upperlink.nibss.cmms.model.emandate.Subscriber(s.id ,s.makerChecker.unapprovedData) from Subscriber s where s.makerChecker.unapprovedData is not null ";
//    String BASE_UNAPPROVED_QUERY = BASE_ALL_JSON_QUERY +" and s.makerChecker.approvalStatus is null ";
//    String BASE_APPROVED_OR_DISAPPROVED_QUERY = BASE_ALL_JSON_QUERY +" and s.makerChecker.approvalStatus = ?1 ";

    @Modifying
    @Query("UPDATE Subscriber s SET s.activated = CASE s.activated WHEN true THEN false ELSE true END WHERE s.id = :id")
    int toggle(@Param("id") Long id);

    @Query("select s from Subscriber s")
    Page<Subscriber> getAll(Pageable pageable);

    @Query("select s from Subscriber s where s.activated =:activated")
    Page<Subscriber> getAllByActivated(@Param("activated") boolean activated,Pageable pageable);

    @Query("select s from Subscriber s where s.mrc = :mrc")
    Subscriber getByMRC(@Param("mrc") String mrc);

    @Query("select s from Subscriber s where s.bank.id = :bankId and s.activated =:activated")
    Page<Subscriber> getAllByBank(@Param("bankId") Long bankId, @Param("activated") boolean activated, Pageable pageable);

    @Query("select count(s.id) from Subscriber s where s.bank.id =:bankId")
    long getCountByBank(@Param("bankId")Long bankId);


    @Query("select count(s.id) from Subscriber s where s.accountNumber =:accNum")
    long getCountAccountNumbers(@Param("accNum")String accNum);

    @Query("select count(s.id) from Subscriber s where s.activated =:activated")
    long getCountByStatus(@Param("activated")boolean activated);
}
