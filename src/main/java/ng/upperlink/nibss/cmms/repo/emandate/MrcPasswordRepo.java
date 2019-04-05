package ng.upperlink.nibss.cmms.repo.emandate;

import ng.upperlink.nibss.cmms.model.emandate.MrcRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;


@Repository
@Transactional
public interface MrcPasswordRepo extends JpaRepository<MrcRecoveryCode,Long> {
    @Query("SELECT r FROM MrcRecoveryCode r WHERE r.recoveryCode = ?1")
    MrcRecoveryCode findRecoveryCode(String recoveryCode);
}
