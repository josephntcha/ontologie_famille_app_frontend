package bj.ifri.parente.domaine;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Personne saisie par l'utilisateur ; individu f:personne_{id} de l'ABox. */
@Entity
@Table(name = "personnes", indexes = @Index(name = "idx_personnes_naissance", columnList = "date_naissance"))
public class Personne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(nullable = false, length = 255)
    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Sexe sexe;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    protected Personne() {
    }

    public Personne(String nom, String prenom, Sexe sexe, LocalDate dateNaissance) {
        this.nom = nom;
        this.prenom = prenom;
        this.sexe = sexe;
        this.dateNaissance = dateNaissance;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public Sexe getSexe() {
        return sexe;
    }

    public void setSexe(Sexe sexe) {
        this.sexe = sexe;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    /** « Prénom Nom », pour les messages. */
    public String nomComplet() {
        return prenom + " " + nom;
    }
}
