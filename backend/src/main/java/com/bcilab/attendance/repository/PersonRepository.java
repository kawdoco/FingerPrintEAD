package com.bcilab.attendance.repository;

import com.bcilab.attendance.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    @Query("select p from Person p left join fetch p.department where p.fingerprintTemplateId = :templateId")
    Optional<Person> findByFingerprintTemplateId(@Param("templateId") Integer templateId);

    @Query("select p from Person p left join fetch p.department order by p.fullName")
    List<Person> findAllWithDepartment();

    @Query("select p from Person p left join fetch p.department where p.id = :id")
    Optional<Person> findByIdWithDepartment(@Param("id") Long id);

    @Query("select p.fingerprintTemplateId from Person p")
    List<Integer> findAllTemplateIds();

    long countByActiveTrue();

    boolean existsByIdNumberIgnoreCase(String idNumber);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByFingerprintTemplateId(Integer fingerprintTemplateId);
    boolean existsByFingerprintTemplateIdAndIdNot(Integer fingerprintTemplateId, Long id);
}
