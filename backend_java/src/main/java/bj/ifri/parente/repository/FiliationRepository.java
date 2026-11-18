package bj.ifri.parente.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bj.ifri.parente.domaine.Filiation;

public interface FiliationRepository extends JpaRepository<Filiation, Long> {

    List<Filiation> findAllByOrderByIdAsc();

    @Query("select count(f) > 0 from Filiation f where f.parent.id = :parentId and f.enfant.id = :enfantId")
    boolean existe(@Param("parentId") Long parentId, @Param("enfantId") Long enfantId);

    @Query("select count(f) from Filiation f where f.enfant.id = :enfantId")
    long nombreDeParents(@Param("enfantId") Long enfantId);

    @Query("select f from Filiation f where f.parent.id = :id or f.enfant.id = :id")
    List<Filiation> concernant(@Param("id") Long personneId);
}
