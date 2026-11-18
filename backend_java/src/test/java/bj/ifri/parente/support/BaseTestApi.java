package bj.ifri.parente.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base commune de tous les tests (unitaires des beans de la section F et d'intégration).
 *
 * <ul>
 *   <li>un seul contexte Spring pour toute la suite (même configuration partout, profil {@code test} :
 *       H2 en mémoire, date de référence 2026-11-18) ;</li>
 *   <li>chaque test s'exécute dans une transaction annulée à la fin (rollback) ;</li>
 *   <li>les données sont créées <b>uniquement par l'API HTTP</b> (SPEC_API_JAVA.md, section E),
 *       via MockMvc ; les helpers renvoient les ids attribués par l'API.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseTestApi {

    /** Espace de noms de l'ontologie (préfixe f:). */
    public static final String NS = "http://www.semanticweb.org/sunday/ontologies/2026/9/untitled-ontology-2#";

    /** Id qui ne correspond à aucune ressource (les ids ne sont jamais réutilisés après rollback). */
    public static final long INEXISTANT = 987_654_321L;

    /** Mapper JSON propre aux tests (indépendant de la configuration Jackson de l'application). */
    protected static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    protected MockMvc mvc;

    private int compteurAncetresGeneres = 0;

    // ------------------------------------------------------------------ HTTP

    protected Reponse get(String url) {
        return executer(MockMvcRequestBuilders.get(url));
    }

    protected Reponse post(String url, Map<String, ?> corps) {
        return executer(avecCorps(MockMvcRequestBuilders.post(url), corps));
    }

    protected Reponse put(String url, Map<String, ?> corps) {
        return executer(avecCorps(MockMvcRequestBuilders.put(url), corps));
    }

    protected Reponse patch(String url, Map<String, ?> corps) {
        return executer(avecCorps(MockMvcRequestBuilders.patch(url), corps));
    }

    protected Reponse delete(String url) {
        return executer(MockMvcRequestBuilders.delete(url));
    }

    private static MockHttpServletRequestBuilder avecCorps(MockHttpServletRequestBuilder requete, Map<String, ?> corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(corps));
    }

    private Reponse executer(MockHttpServletRequestBuilder requete) {
        try {
            MockHttpServletResponse r = mvc.perform(requete).andReturn().getResponse();
            String corps = r.getContentAsString(StandardCharsets.UTF_8);
            String type = r.getContentType();
            JsonNode json = null;
            String debut = corps.stripLeading();
            if (debut.startsWith("{") || debut.startsWith("[")) {
                try {
                    json = JSON.readTree(corps);
                } catch (RuntimeException e) {
                    json = null;
                }
            }
            return new Reponse(r.getStatus(), type, corps, json);
        } catch (Exception e) {
            throw new IllegalStateException("Échec de la requête MockMvc", e);
        }
    }

    /**
     * Corps de requête à partir de paires clé/valeur (les valeurs null sont conservées).
     * Exemple : {@code corps("parent_id", 1L, "enfant_id", 2L)}.
     */
    protected static Map<String, Object> corps(Object... clesValeurs) {
        if (clesValeurs.length % 2 != 0) {
            throw new IllegalArgumentException("Nombre pair d'arguments attendu");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < clesValeurs.length; i += 2) {
            m.put((String) clesValeurs[i], clesValeurs[i + 1]);
        }
        return m;
    }

    // ------------------------------------------------------------------ assertions

    protected static void assertStatut(Reponse r, int attendu) {
        assertThat(r.statut()).as("Statut HTTP inattendu, corps : %s", r.corps()).isEqualTo(attendu);
    }

    /**
     * 422 au format { "message": "...", "errors": { "champ": ["...", ...] } } avec au moins
     * les champs indiqués (SPEC_API_JAVA.md, section B). D'autres clés peuvent être présentes.
     */
    protected static void assertErreurs422(Reponse r, String... champs) {
        assertStatut(r, 422);
        assertThat(r.json()).as("Corps JSON attendu : %s", r.corps()).isNotNull();
        assertThat(r.json().path("message").isString()).as("message attendu : %s", r.corps()).isTrue();
        JsonNode erreurs = r.json().path("errors");
        assertThat(erreurs.isObject()).as("objet errors attendu : %s", r.corps()).isTrue();
        for (String champ : champs) {
            JsonNode messages = erreurs.path(champ);
            assertThat(messages.isArray() && !messages.isEmpty())
                    .as("Clé d'erreur '%s' attendue (tableau non vide) dans %s", champ, r.corps())
                    .isTrue();
        }
    }

    /** 404 avec { "message": "..." } (SPEC_API_JAVA.md, section B). */
    protected static void assert404(Reponse r) {
        assertStatut(r, 404);
        assertThat(r.json()).as("Corps JSON attendu : %s", r.corps()).isNotNull();
        assertThat(r.json().path("message").isString()).as("message attendu : %s", r.corps()).isTrue();
    }

    /** Ids (champ "id") d'un tableau JSON d'objets, triés. */
    protected static List<Long> ids(JsonNode tableau) {
        assertThat(tableau.isArray()).as("Tableau JSON attendu : %s", tableau).isTrue();
        List<Long> r = new ArrayList<>();
        for (JsonNode n : tableau) {
            r.add(n.get("id").asLong());
        }
        r.sort(null);
        return r;
    }

    /** Liste triée d'ids. */
    protected static List<Long> idsTries(Long... ids) {
        List<Long> r = new ArrayList<>(Arrays.asList(ids));
        r.sort(null);
        return r;
    }

    /** Chaînes d'un tableau JSON, dans l'ordre. */
    protected static List<String> chaines(JsonNode tableau) {
        assertThat(tableau.isArray()).as("Tableau JSON attendu : %s", tableau).isTrue();
        List<String> r = new ArrayList<>();
        for (JsonNode n : tableau) {
            r.add(n.stringValue());
        }
        return r;
    }

    /** URI d'une personne dans l'ontologie (SPEC_API.md, section 2). */
    protected static String uriPersonne(Long id) {
        return NS + "personne_" + id;
    }

    // ------------------------------------------------------------------ création des données par l'API

    /** POST /api/personnes ; renvoie l'id. */
    protected Long personne(String prenom, String sexe, String naissance, String nom) {
        Reponse r = post("/api/personnes", corps("nom", nom, "prenom", prenom, "sexe", sexe, "date_naissance", naissance));
        assertStatut(r, 201);
        return r.entier("/data/id");
    }

    /** POST /api/personnes avec le nom "Nom" + prénom ; renvoie l'id. */
    protected Long personne(String prenom, String sexe, String naissance) {
        return personne(prenom, sexe, naissance, "Nom" + prenom);
    }

    /** POST /api/liens/parente (parentId parentDe enfantId) ; renvoie l'id du lien. */
    protected Long parent(Long parentId, Long enfantId) {
        Reponse r = post("/api/liens/parente", corps("parent_id", parentId, "enfant_id", enfantId));
        assertStatut(r, 201);
        return r.entier("/data/id");
    }

    /** POST /api/liens/beau-parente (beauParentId beauParentDe enfantId) ; renvoie l'id du lien. */
    protected Long beauParent(Long beauParentId, Long enfantId) {
        Reponse r = post("/api/liens/beau-parente", corps("beau_parent_id", beauParentId, "enfant_id", enfantId));
        assertStatut(r, 201);
        return r.entier("/data/id");
    }

    /** POST /api/mariages d'un mariage en cours ; renvoie l'id. */
    protected Long mariage(Long conjoint1, Long conjoint2, String debut) {
        return mariage(conjoint1, conjoint2, debut, null, null);
    }

    /** POST /api/mariages (fin et cause optionnelles, null pour un mariage en cours) ; renvoie l'id. */
    protected Long mariage(Long conjoint1, Long conjoint2, String debut, String fin, String cause) {
        Map<String, Object> c = corps("conjoint1_id", conjoint1, "conjoint2_id", conjoint2, "date_debut", debut);
        if (fin != null) {
            c.put("date_fin", fin);
        }
        if (cause != null) {
            c.put("cause_dissolution", cause);
        }
        Reponse r = post("/api/mariages", c);
        assertStatut(r, 201);
        return r.entier("/data/id");
    }

    /** PATCH /api/mariages/{id} : dissolution du mariage. */
    protected void dissoudre(Long mariageId, String fin, String cause) {
        Reponse r = patch("/api/mariages/" + mariageId, corps("date_fin", fin, "cause_dissolution", cause));
        assertStatut(r, 200);
    }

    /** Famille de démonstration (voir {@link FamilleDemo}), créée par l'API. */
    protected FamilleDemo familleDemo() {
        Long grandpa = personne("Grandpa", "M", "1930-03-10", "Martin");
        Long grandma = personne("Grandma", "F", "1932-07-21", "Martin");
        Long papa = personne("Papa", "M", "1958-05-02", "Martin");
        Long tonton = personne("Tonton", "M", "1960-09-15", "Martin");
        Long maman = personne("Maman", "F", "1962-02-14", "Durand");
        Long autre = personne("Autre", "M", "1955-11-30", "Bernard");
        Long demi = personne("Demi", "M", "1982-08-30", "Bernard");
        Long tantine = personne("Tantine", "F", "1963-12-01", "Petit");
        Long kid = personne("Kid", "F", "1990-04-12", "Martin");
        Long cousin = personne("Cousin", "M", "1991-10-05", "Martin");

        parent(grandpa, papa);
        parent(grandma, papa);
        parent(grandpa, tonton);
        parent(grandma, tonton);
        parent(papa, kid);
        parent(maman, kid);
        parent(maman, demi);
        parent(autre, demi);
        parent(tonton, cousin);
        parent(tantine, cousin);

        Long mariagePapaMaman = mariage(papa, maman, "1988-06-01");
        Long mariageTontonTantine = mariage(tonton, tantine, "1989-09-09", "2010-03-15", "divorce");

        return new FamilleDemo(grandpa, grandma, papa, tonton, maman, autre, demi, tantine, kid, cousin,
                mariagePapaMaman, mariageTontonTantine);
    }

    /**
     * Complète l'ascendance de la personne sur 2 générations : 2 parents, puis 2 parents pour
     * chacun de ses parents, en créant uniquement des personnes neuves et isolées (aucun lien
     * de sang ni d'alliance parasite), nées 30 ans avant leur enfant.
     */
    protected void completerAscendance(Long personneId) {
        for (Long parentId : completerParents(personneId)) {
            completerParents(parentId);
        }
    }

    /** Donne 2 parents à la personne (en créant ceux qui manquent) et renvoie ses 2 parents. */
    private List<Long> completerParents(Long personneId) {
        Map<Long, JsonNode> personnes = new HashMap<>();
        Reponse liste = get("/api/personnes");
        assertStatut(liste, 200);
        for (JsonNode p : liste.at("/data")) {
            personnes.put(p.get("id").asLong(), p);
        }
        Reponse liens = get("/api/liens/parente");
        assertStatut(liens, 200);
        List<Long> parents = new ArrayList<>();
        for (JsonNode l : liens.at("/data")) {
            if (l.get("enfant_id").asLong() == personneId) {
                parents.add(l.get("parent_id").asLong());
            }
        }
        List<String> sexesManquants = new ArrayList<>();
        if (parents.isEmpty()) {
            sexesManquants.addAll(List.of("M", "F"));
        } else if (parents.size() == 1) {
            String sexePresent = personnes.get(parents.get(0)).get("sexe").stringValue();
            sexesManquants.add("M".equals(sexePresent) ? "F" : "M");
        }
        LocalDate naissanceEnfant = LocalDate.parse(personnes.get(personneId).get("date_naissance").stringValue());
        String naissance = naissanceEnfant.minusYears(30).toString();
        for (String sexe : sexesManquants) {
            int n = ++compteurAncetresGeneres;
            Long nouveau = personne("Anc" + n, sexe, naissance, "Ancetre" + n + "Genere");
            parent(nouveau, personneId);
            parents.add(nouveau);
        }
        return parents;
    }
}
