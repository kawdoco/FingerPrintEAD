package com.bcilab.attendance.repository;

import com.bcilab.attendance.model.AttendanceRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    String FETCH = "select a from AttendanceRecord a join fetch a.person p left join fetch p.department left join fetch a.device ";

    @Query(FETCH + "where a.scannedAt >= :from order by a.scannedAt desc")
    List<AttendanceRecord> findLatestSince(@Param("from") OffsetDateTime from, Pageable pageable);

    @Query(FETCH + "where a.scannedAt >= :from and a.scannedAt < :to order by a.scannedAt desc")
    List<AttendanceRecord> findAllBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(FETCH + "where p.id = :personId and a.scannedAt >= :from and a.scannedAt < :to order by a.scannedAt desc")
    List<AttendanceRecord> findByPersonBetween(@Param("personId") Long personId,
                                                @Param("from") OffsetDateTime from,
                                                @Param("to") OffsetDateTime to);
}
