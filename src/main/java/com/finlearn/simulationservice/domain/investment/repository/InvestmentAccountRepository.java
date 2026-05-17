package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvestmentAccountRepository extends JpaRepository<InvestmentAccount, UUID> {

    Optional<InvestmentAccount> findByParticipant_InvestorIdAndParticipant_SeasonId(UUID investorId, UUID seasonId);

    Optional<InvestmentAccount> findByParticipant_InvestorIdAndStatus(UUID investorId, InvestmentAccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select account
            from InvestmentAccount account
            where account.participant.investorId = :investorId
              and account.status = :status
            """)
    Optional<InvestmentAccount> findByInvestorIdAndStatusForUpdate(
            @Param("investorId") UUID investorId,
            @Param("status") InvestmentAccountStatus status
    );
}
