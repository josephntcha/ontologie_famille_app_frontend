package bj.ifri.parente.domaine;

import java.time.LocalDate;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Mariage daté (individu f:Mariage de l'ABox) entre deux conjoints. Il est <b>en cours</b> tant
 * qu'il n'a pas de date de fin (art. 125 et 143) : OWL ne peut pas le déduire (monde ouvert).
 */
@Entity
@Table(name = "mariages")
public class Mariage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conjoint1_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Personne conjoint1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conjoint2_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Personne conjoint2;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "cause_dissolution", length = 10)
    private CauseDissolution causeDissolution;

    protected Mariage() {
    }

    public Mariage(Personne conjoint1, Personne conjoint2, LocalDate dateDebut, LocalDate dateFin,
            CauseDissolution causeDissolution) {
        this.conjoint1 = conjoint1;
        this.conjoint2 = conjoint2;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.causeDissolution = causeDissolution;
    }

    public Long getId() {
        return id;
    }

    public Long getConjoint1Id() {
        return conjoint1.getId();
    }

    public Long getConjoint2Id() {
        return conjoint2.getId();
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public CauseDissolution getCauseDissolution() {
        return causeDissolution;
    }

    /** Enregistre la dissolution (date de fin et cause). */
    public void dissoudre(LocalDate fin, CauseDissolution cause) {
        this.dateFin = fin;
        this.causeDissolution = cause;
    }

    public boolean estEnCours() {
        return dateFin == null;
    }
}
