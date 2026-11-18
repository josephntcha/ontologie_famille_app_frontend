package bj.ifri.parente.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bj.ifri.parente.domaine.BeauParente;

public interface BeauParenteRepository extends JpaRepository<BeauParente, Long> {

    List<BeauParente> findAllByOrderByIdAsc();

    @Query("select count(b) > 0 from BeauParente b where b.beauParent.id = :beauParentId and b.enfant.id = :enfantId")
    boolean existe(@Param("beauParentId") Long beauParentId, @Param("enfantId") Long enfantId);

    @Query("select b from BeauParente b where b.beauParent.id = :id or b.enfant.id = :id")
    List<BeauParente> concernant(@Param("id") Long personneId);
}
