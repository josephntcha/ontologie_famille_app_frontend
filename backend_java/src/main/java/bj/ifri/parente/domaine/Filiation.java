package bj.ifri.parente.domaine;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Lien f:parentDe (parent parentDe enfant). */
@Entity
@Table(name = "filiations", uniqueConstraints = @UniqueConstraint(name = "uk_filiation", columnNames = {"parent_id", "enfant_id"}))
public class Filiation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Personne parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enfant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Personne enfant;

    protected Filiation() {
    }

    public Filiation(Personne parent, Personne enfant) {
        this.parent = parent;
        this.enfant = enfant;
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parent.getId();
    }

    public Long getEnfantId() {
        return enfant.getId();
    }
}
