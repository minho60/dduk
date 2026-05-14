package com.dduk.domain.accounting.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    boolean existsByDescription(String description);
    boolean existsBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
