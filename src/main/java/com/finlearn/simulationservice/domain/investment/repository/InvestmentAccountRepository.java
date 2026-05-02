package com.finlearn.simulationservice.domain.investment.repository;

import com.finlearn.simulationservice.domain.investment.entity.InvestmentAccount;
import com.finlearn.simulationservice.domain.investment.enums.InvestmentAccountStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentAccountRepository extends JpaRepository<InvestmentAccount, UUID> {

    Optional<InvestmentAccount> findByParticipant_InvestorIdAndParticipant_SeasonId(UUID investorId, UUID seasonId);

    Optional<InvestmentAccount> findByParticipant_InvestorIdAndStatus(UUID investorId, InvestmentAccountStatus status);
}
