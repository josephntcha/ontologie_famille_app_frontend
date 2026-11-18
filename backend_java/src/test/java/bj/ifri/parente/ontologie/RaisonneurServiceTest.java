package bj.ifri.parente.ontologie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.ClassesAttendues;
import bj.ifri.parente.support.FamilleDemo;

/**
 * RaisonneurService : classes inférées par HermiT (SPEC_API_JAVA.md C), lienDeSangAvec
 * inféré (D.1) et cohérence (D.2). Les listes attendues ont été obtenues en exécutant HermiT
 * sur l'ABox de la section C (voir {@link ClassesAttendues}).
 */
@DisplayName("RaisonneurService (HermiT)")
class RaisonneurServiceTest extends BaseTestApi {

    @Autowired
    private RaisonneurService raisonneur;

    // ------------------------------------------------------------------ classesPersonne : famille de démonstration

    @Test
    @DisplayName("Grandpa et Grandma : grands-parents (listes exactes et triées)")
    void classesDesGrandsParents() {
        FamilleDemo f = familleDemo();

        assertThat(raisonneur.classesPersonne(f.grandpa())).containsExactlyElementsOf(ClassesAttendues.GRANDPA);
        assertThat(raisonneur.classesPersonne(f.grandma())).containsExactlyElementsOf(ClassesAttendues.GRANDMA);
    }

    @Test
    @DisplayName("Papa et Tonton : fils, frères, pères et oncles")
    void classesDePapaEtTonton() {
        FamilleDemo f = familleDemo();

        assertThat(raisonneur.classesPersonne(f.papa())).containsExactlyElementsOf(ClassesAttendues.PAPA);
        assertThat(raisonneur.classesPersonne(f.tonton())).containsExactlyElementsOf(ClassesAttendues.TONTON);
    }

    @Test
    @DisplayName("Kid : cousine, nièce, petite-fille et sœur, mais pas DemiSoeur sans beauParentDe")
    void classesDeKid() {
        FamilleDemo f = familleDemo();

        List<String> classes = raisonneur.classesPersonne(f.kid());

        assertThat(classes).containsExactlyElementsOf(ClassesAttendues.KID);
        assertThat(classes).doesNotContain("DemiSoeur", "Masculin", "Fillette", "Tante");
    }

    @Test
    @DisplayName("Cousin : cousin, neveu et petit-fils")
    void classesDeCousin() {
        FamilleDemo f = familleDemo();

        assertThat(raisonneur.classesPersonne(f.cousin())).containsExactlyElementsOf(ClassesAttendues.COUSIN);
    }

    @Test
    @DisplayName("Maman et Tantine : BelleFille (épouses d'un Fils, même après divorce) ; Autre et Demi")
    void classesDesAutresMembres() {
        FamilleDemo f = familleDemo();

        assertThat(raisonneur.classesPersonne(f.maman())).containsExactlyElementsOf(ClassesAttendues.MAMAN);
        assertThat(raisonneur.classesPersonne(f.tantine())).containsExactlyElementsOf(ClassesAttendues.TANTINE);
        assertThat(raisonneur.classesPersonne(f.autre())).containsExactlyElementsOf(ClassesAttendues.AUTRE);
        assertThat(raisonneur.classesPersonne(f.demi())).containsExactlyElementsOf(ClassesAttendues.DEMI);
    }

    // ------------------------------------------------------------------ classesPersonne : variantes

    @Test
    @DisplayName("Beau-père isolé déclaré beauParentDe : BeauParent et BeauPere")
    void classesDUnBeauPereIsole() {
        FamilleDemo f = familleDemo();
        Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
        beauParent(beaupere, f.kid());

        assertThat(raisonneur.classesPersonne(beaupere)).containsExactlyElementsOf(ClassesAttendues.BEAU_PERE_ISOLE);
        assertThat(raisonneur.classesPersonne(f.kid())).containsExactlyElementsOf(ClassesAttendues.KID);
    }

    @Test
    @DisplayName("DemiFrere / DemiSoeur suivent la définition OWL (enfant d'un BeauParent ou d'un conjoint de BeauParent)")
    void demiFrereEtDemiSoeurSelonLaDefinitionOwl() {
        FamilleDemo f = familleDemo();
        // Papa, époux de Maman, devient beau-parent de Demi (fils de Maman et d'Autre).
        beauParent(f.papa(), f.demi());

        // Demi : enfant de Maman, mariée à Papa qui est BeauParent et Parent.
        assertThat(raisonneur.classesPersonne(f.demi())).contains("DemiFrere", "Frere");
        // Kid : enfant de Papa, qui est BeauParent.
        assertThat(raisonneur.classesPersonne(f.kid())).contains("DemiSoeur", "Soeur");
        assertThat(raisonneur.classesPersonne(f.papa())).contains("BeauParent", "BeauPere", "Pere");
        // Cousin n'est pas concerné.
        assertThat(raisonneur.classesPersonne(f.cousin())).doesNotContain("DemiFrere");
    }

    @Test
    @DisplayName("BeauFils dès que l'épouse a un parent connu (elle devient Fille)")
    void beauFilsQuandLEpouseEstUneFille() {
        FamilleDemo f = familleDemo();
        assertThat(raisonneur.classesPersonne(f.papa())).doesNotContain("BeauFils");

        Long pereDeMaman = personne("PereMaman", "M", "1932-02-14", "Durand");
        parent(pereDeMaman, f.maman());

        assertThat(raisonneur.classesPersonne(f.papa())).contains("BeauFils");
        assertThat(raisonneur.classesPersonne(f.maman())).contains("Fille", "BelleFille");
        assertThat(raisonneur.classesPersonne(f.demi())).contains("PetitFils");
        // Tantine n'a toujours pas de parent connu : Tonton n'est pas BeauFils.
        assertThat(raisonneur.classesPersonne(f.tonton())).doesNotContain("BeauFils");
    }

    @Test
    @DisplayName("Tante : sœur d'un parent")
    void tante() {
        FamilleDemo f = familleDemo();
        Long tatie = personne("Tatie", "F", "1965-01-20", "Martin");
        parent(f.grandpa(), tatie);
        parent(f.grandma(), tatie);

        assertThat(raisonneur.classesPersonne(tatie)).containsExactlyElementsOf(ClassesAttendues.TATIE);
    }

    @Test
    @DisplayName("Demi devient Oncle quand sa demi-sœur Kid devient parent (frereDe déduit des parents communs)")
    void demiFrereDevientOncle() {
        FamilleDemo f = familleDemo();
        assertThat(raisonneur.classesPersonne(f.demi())).doesNotContain("Oncle");

        Long bebe = personne("Bebe", "M", "2015-06-01", "Martin");
        parent(f.kid(), bebe);

        assertThat(raisonneur.classesPersonne(f.demi())).contains("Oncle");
        assertThat(raisonneur.classesPersonne(f.kid())).contains("Mere", "Parent");
        assertThat(raisonneur.classesPersonne(bebe)).contains("Neveu", "PetitFils", "Garcon").doesNotContain("Homme");
        assertThat(raisonneur.classesPersonne(f.papa())).contains("GrandPere");
    }

    @Test
    @DisplayName("Homme / Femme (âge >= 21) et Garcon / Fillette (âge <= 20) selon l'âge au 2026-11-18")
    void classesSelonLAge() {
        Long garcon20 = personne("Vingt", "M", "2005-11-19");
        Long homme21 = personne("VingtEtUn", "M", "2005-11-18");
        Long fillette20 = personne("Vingte", "F", "2005-11-19");
        Long femme21 = personne("VingtEtUne", "F", "2005-11-18");

        assertThat(raisonneur.classesPersonne(garcon20)).containsExactly("Garcon", "Masculin", "Personne", "Sexe");
        assertThat(raisonneur.classesPersonne(homme21)).containsExactly("Homme", "Masculin", "Personne", "Sexe");
        assertThat(raisonneur.classesPersonne(fillette20)).containsExactly("Feminin", "Fillette", "Personne", "Sexe");
        assertThat(raisonneur.classesPersonne(femme21)).containsExactly("Feminin", "Femme", "Personne", "Sexe");
    }

    @Test
    @DisplayName("Pas de cache : les classes suivent les données à chaque appel")
    void pasDeCacheEntreDeuxAppels() {
        Long x = personne("Xavier", "M", "1960-01-01");
        Long y = personne("Yann", "M", "1990-01-01");
        assertThat(raisonneur.classesPersonne(x)).doesNotContain("Parent");

        parent(x, y);

        assertThat(raisonneur.classesPersonne(x)).contains("Parent", "Pere");
        assertThat(raisonneur.classesPersonne(y)).contains("Enfant", "Fils");
    }

    // ------------------------------------------------------------------ classesMariage

    @Test
    @DisplayName("classesMariage : Mariage, MariageDissous, MariageDissousParDeces / ParDivorce")
    void classesDesMariages() {
        FamilleDemo f = familleDemo();
        Long a = personne("Albert", "M", "1970-01-01", "Alpha");
        Long b = personne("Berthe", "F", "1972-01-01", "Beta");
        Long deces = mariage(a, b, "1995-01-01", "2005-03-03", "deces");

        assertThat(raisonneur.classesMariage(f.mariagePapaMaman())).containsExactlyElementsOf(ClassesAttendues.MARIAGE_EN_COURS);
        assertThat(raisonneur.classesMariage(f.mariageTontonTantine())).containsExactlyElementsOf(ClassesAttendues.MARIAGE_DIVORCE);
        assertThat(raisonneur.classesMariage(deces)).containsExactlyElementsOf(ClassesAttendues.MARIAGE_DECES);
    }

    @Test
    @DisplayName("classesMariage : suit une dissolution enregistrée après coup")
    void classesMariageApresDissolution() {
        FamilleDemo f = familleDemo();
        dissoudre(f.mariagePapaMaman(), "2020-01-10", "deces");

        assertThat(raisonneur.classesMariage(f.mariagePapaMaman())).containsExactlyElementsOf(ClassesAttendues.MARIAGE_DECES);
    }

    // ------------------------------------------------------------------ lienDeSangInfere

    @Test
    @DisplayName("lienDeSangInfere : vrai en ligne directe et en collatérale, dans les deux sens")
    void lienDeSangInfereVrai() {
        FamilleDemo f = familleDemo();

        assertThat(raisonneur.lienDeSangInfere(f.kid(), f.papa())).isTrue();
        assertThat(raisonneur.lienDeSangInfere(f.papa(), f.kid())).isTrue();
        assertThat(raisonneur.lienDeSangInfere(f.grandpa(), f.kid())).isTrue();
        assertThat(raisonneur.lienDeSangInfere(f.papa(), f.tonton())).isTrue();
        assertThat(raisonneur.lienDeSangInfere(f.kid(), f.demi())).isTrue();
        assertThat(raisonneur.lienDeSangInfere(f.tonton(), f.kid())).isTrue();
        assertThat(raisonneur.lienDeSangInfere(f.kid(), f.cousin())).isTrue();
        assertThat(raisonneur.lienDeSangInfere(f.cousin(), f.kid())).isTrue();
    }

    @Test
    @DisplayName("lienDeSangInfere : faux sans ancêtre commun, entre époux et pour une simple alliance")
    void lienDeSangInfereFaux() {
        FamilleDemo f = familleDemo();
        Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
        beauParent(beaupere, f.kid());

        assertThat(raisonneur.lienDeSangInfere(f.demi(), f.cousin())).isFalse();
        assertThat(raisonneur.lienDeSangInfere(f.papa(), f.maman())).isFalse();
        assertThat(raisonneur.lienDeSangInfere(f.grandpa(), f.maman())).isFalse();
        assertThat(raisonneur.lienDeSangInfere(f.tonton(), f.maman())).isFalse();
        assertThat(raisonneur.lienDeSangInfere(f.tantine(), f.kid())).isFalse();
        assertThat(raisonneur.lienDeSangInfere(beaupere, f.kid())).isFalse();
    }

    // ------------------------------------------------------------------ verifierCoherence

    @Test
    @DisplayName("verifierCoherence : base vide cohérente, aucune classe insatisfiable")
    void coherenceBaseVide() {
        Coherence c = raisonneur.verifierCoherence();

        assertThat(c.coherente()).isTrue();
        assertThat(c.classesInsatisfiables()).isEmpty();
    }

    @Test
    @DisplayName("verifierCoherence : famille recomposée avec mariages dissous et beau-parent, cohérente")
    void coherenceFamilleDemo() {
        FamilleDemo f = familleDemo();
        beauParent(f.papa(), f.demi());
        Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
        beauParent(beaupere, f.cousin());
        dissoudre(f.mariagePapaMaman(), "2020-01-10", "deces");

        Coherence c = raisonneur.verifierCoherence();

        assertThat(c.coherente()).isTrue();
        assertThat(c.classesInsatisfiables()).isEmpty();
    }
}
