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

/** Lien f:beauParentDe (beau-parent beauParentDe enfant). */
@Entity
@Table(name = "beau_parentes", uniqueConstraints = @UniqueConstraint(name = "uk_beau_parente", columnNames = {"beau_parent_id", "enfant_id"}))
public class BeauParente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beau_parent_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Personne beauParent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enfant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Personne enfant;

    protected BeauParente() {
    }

    public BeauParente(Personne beauParent, Personne enfant) {
        this.beauParent = beauParent;
        this.enfant = enfant;
    }

    public Long getId() {
        return id;
    }

    public Long getBeauParentId() {
        return beauParent.getId();
    }

    public Long getEnfantId() {
        return enfant.getId();
    }
}
