package bj.ifri.parente.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bj.ifri.parente.domaine.Mariage;

public interface MariageRepository extends JpaRepository<Mariage, Long> {

    List<Mariage> findAllByOrderByIdAsc();

    @Query("select m from Mariage m where m.conjoint1.id = :id or m.conjoint2.id = :id order by m.id")
    List<Mariage> concernant(@Param("id") Long personneId);

    /** Vrai si la personne a un mariage sans date de fin (en cours). */
    @Query("select count(m) > 0 from Mariage m where (m.conjoint1.id = :id or m.conjoint2.id = :id) and m.dateFin is null")
    boolean aUnMariageEnCours(@Param("id") Long personneId);
}
