package com.md287.risk.repository;

import com.md287.risk.domain.Assessment;
import com.md287.risk.domain.Disposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, String> {

    Optional<Assessment> findByTransactionId(String transactionId);

    List<Assessment> findByDisposition(Disposition disposition);
}
