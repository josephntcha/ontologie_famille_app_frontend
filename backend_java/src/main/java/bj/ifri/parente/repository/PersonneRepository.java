package bj.ifri.parente.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bj.ifri.parente.domaine.Personne;

public interface PersonneRepository extends JpaRepository<Personne, Long> {

    List<Personne> findAllByOrderByIdAsc();

    List<Personne> findByDateNaissanceOrderByIdAsc(LocalDate dateNaissance);
}
