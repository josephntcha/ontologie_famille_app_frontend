# Contrat de l'API Spring Boot – Détection des liens de parenté (MVP, version Java + HermiT)

Ce document complète `SPEC_API.md` pour le projet `backend_java/`. **Le contrat HTTP des sections 2 à 9 de `SPEC_API.md` s'applique à l'identique** (routes, corps, JSON en snake_case, codes HTTP, clés d'erreur 422, algorithmes du lien de sang, nommage, alliance, règles de mariage, complétude, arbre, ontologie, export Turtle), **sauf** les différences listées ici. Les sections 1 (modèles Eloquent) et 10 (services PHP) de `SPEC_API.md` ne s'appliquent pas : elles sont remplacées par les sections A et F ci-dessous.

Différence de fond avec le projet Laravel : ici, **l'ontologie OWL est exploitée par un vrai raisonneur (HermiT, via la OWL API)**. Les classes déduites ne sont pas codées à la main : elles sont **inférées par HermiT** à partir de la TBox (`famille.owl`) et de l'ABox construite depuis les données. Le degré et le nom du lien restent calculés dans le code, comme le prévoit le MVP (OWL dit si deux personnes sont liées, pas à combien de générations).

---

## A. Socle technique

- Projet Maven dans `backend_java/`, Java 25, Spring Boot (dernière version stable), package racine **`bj.ifri.parente`**, classe principale `bj.ifri.parente.ParenteApplication`.
- Dépendances : spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, H2, spring-boot-starter-test ; **OWL API 5** (`net.sourceforge.owlapi:owlapi-distribution`) et **HermiT** (`net.sourceforge.owlapi:org.semanticweb.hermit`, version compatible OWL API 5).
- Persistance : JPA + H2 (fichier `./data/parente` en dev ; mémoire pour les tests). Les tables et entités sont libres (non testées directement).
- Ontologie : `src/main/resources/ontologie/famille.owl` (copie identique de `web_semantiq_famille_ex.owl`), chargée par la OWL API depuis le classpath.
- **Date de référence** (« aujourd'hui », pour l'âge, les règles d'âge et la validation « date pas dans le futur ») : propriété `app.date-reference` (format `yyyy-MM-dd`) si elle est définie, sinon `LocalDate.now()`.
- **Pas de cache des données ou du raisonnement entre deux appels** : l'ABox et le raisonneur sont reconstruits à partir de la base à chaque requête qui en a besoin (dans une même requête, on peut réutiliser l'instance). Les tests utilisent des transactions annulées (rollback) : un cache invalidé à l'écriture deviendrait périmé.

## B. Conventions HTTP (identiques au projet Laravel)

- JSON en **snake_case** dans les deux sens (`date_naissance`, `parent_id`, `doublons_probables`, …).
- Dates au format `yyyy-MM-dd`.
- Erreur de validation ou de règle métier : **HTTP 422**, corps `{ "message": "...", "errors": { "<champ>": ["...", ...] } }`. Les champs sont nommés en snake_case comme dans les requêtes. Les clés à utiliser pour chaque règle sont celles de `SPEC_API.md`.
- Champ requis absent ou mauvais type (ex. `sexe` = `"X"`, date invalide) : 422 avec le champ en clé (pas 400).
- Id d'un corps de requête qui ne correspond à aucune personne (ex. `parent_id: 9999`) : 422 avec ce champ en clé.
- Ressource d'URL inexistante (`GET /api/personnes/9999`, `DELETE /api/mariages/9999`…) : **404** avec `{ "message": "..." }`.
- Création : 201. Suppression : 204 sans corps.
- `PUT` et `PATCH /api/personnes/{id}` acceptent tous deux une mise à jour partielle.

## C. Classes déduites par HermiT (remplace la section 7 de SPEC_API.md)

Pour chaque appel qui renvoie des `classes` (détail d'une personne, nœuds de l'arbre, mariages), l'application :
1. charge la TBox `famille.owl` ;
2. construit l'ABox à partir de la base, avec l'espace de noms de l'ontologie :
   - chaque personne : individu `f:personne_{id}`, typé `f:Personne` et `f:Masculin` ou `f:Feminin`, avec `f:nom`, `f:prenom` (xsd:string), `f:dateNaissance` (xsd:dateTime, `yyyy-MM-ddT00:00:00`) et **`f:age` (xsd:int) calculé par l'application** à la date de référence ;
   - `f:parentDe` pour chaque filiation, `f:beauParentDe` pour chaque beau-lien ;
   - **`f:frereDe` / `f:soeurDe` ajoutés par l'application** avant le raisonnement, entre deux personnes qui partagent au moins un parent (frereDe si le sujet est masculin, soeurDe s'il est féminin) — OWL ne sait pas les déduire (MVP) ;
   - chaque mariage : individu `f:mariage_{id}` typé `f:Mariage`, `f:aPourConjoint` vers les deux conjoints, `f:dateDebut` (xsd:dateTime), `f:dateFin` et `f:causeDissolution` (xsd:string `"deces"`/`"divorce"`) si renseignés ; et `f:marieA` entre les deux conjoints (propriété symétrique), que le mariage soit en cours ou dissous ;
   - les individus sont déclarés différents deux à deux (`DifferentIndividuals`) ;
3. lance **HermiT** et renvoie, pour l'individu demandé, **tous les noms locaux des classes nommées de l'ontologie dont il est instance** (types directs et indirects inférés), sans `owl:Thing`, triés par ordre alphabétique (`String.compareTo`, sensible à la casse), sans doublon.

Le résultat est exactement celui de HermiT sur la TBox fournie : par exemple `Masculin ⊑ Sexe` fait apparaître `Sexe`, et `DemiFrere` / `DemiSoeur` suivent la définition OWL (enfant d'un `BeauParent`, ou enfant d'une personne mariée à un `BeauParent` qui est aussi `Parent`), et non la notion « un seul parent commun » utilisée par l'algorithme de vérification (`demi`). Les classes de mariage (`Mariage`, `MariageDissous`, `MariageDissousParDeces`, `MariageDissousParDivorce`) viennent aussi de HermiT.

## D. Ajouts au contrat HTTP

1. Dans la réponse de `POST /api/verification`, `lien_de_sang` contient en plus **`"infere_par_raisonneur": true|false`** : vrai si HermiT infère `f:lienDeSangAvec(personne1, personne2)` (axiomes `ancetreDe ⊑ lienDeSangAvec` et chaîne `descendantDe ∘ ancetreDe ⊑ lienDeSangAvec`). Il doit être cohérent avec `existe` sur des données sans erreur.
2. **`GET /api/raisonneur/coherence`** → 200 `{ "raisonneur": "HermiT", "coherente": true, "classes_insatisfiables": [] }` : cohérence de TBox + ABox actuelle, et liste triée des noms locaux des classes insatisfiables (hors `owl:Nothing`).
3. `GET /api/ontologie` : les listes viennent de la **signature de la TBox lue par la OWL API** (entités nommées dans l'espace de noms de l'ontologie, noms locaux triés, sans doublon), donc 39 classes, 22 propriétés objet et 7 propriétés de données.
4. `GET /api/export/turtle` : même contenu que la section 9 de `SPEC_API.md` (ABox assertée, sans les inférences). Le document doit pouvoir être relu par la OWL API (format Turtle valide).

## E. Données de test

Les tests créent les données **uniquement par l'API HTTP** (MockMvc), jamais par les entités ou les repositories. Ils peuvent ensuite appeler les services de la section F avec les ids renvoyés par l'API.

## F. Services (contrat des tests unitaires)

Beans Spring injectables (`@Autowired`), avec exactement ces noms, paquets et signatures :

| Classe | Méthodes |
|---|---|
| `bj.ifri.parente.service.NommageLien` | `LienNomme nommer(int g1, int g2, boolean demi)` — section 5.2 de SPEC_API.md |
| `bj.ifri.parente.service.LienNomme` | `record LienNomme(String code, String libelle, int degre, String type)` ; `type` = `"ligne_directe"` ou `"collaterale"` ; `degre` = g1 + g2 |
| `bj.ifri.parente.service.GenealogieService` | `Map<Long, Integer> ancetres(Long personneId)` (id → génération minimale, sans la personne) ; `Map<Long, Integer> descendants(Long personneId)` ; `boolean creeraitUnCycle(Long parentId, Long enfantId)` ; `Set<Long> freresEtSoeurs(Long personneId)` (≥ 1 parent commun, sans la personne) |
| `bj.ifri.parente.service.CompletudeService` | `Completude evaluer(Long personneId)` |
| `bj.ifri.parente.service.Completude` | `record Completude(int parentsConnus, int parentsAttendus, int grandsParentsConnus, int grandsParentsAttendus, int generationsCompletes, boolean complet)` |
| `bj.ifri.parente.ontologie.OntologieService` | `String namespace()` ; `List<String> classes()` ; `List<String> proprietesObjet()` ; `List<String> proprietesDonnees()` ; `org.semanticweb.owlapi.model.OWLOntology tbox()` |
| `bj.ifri.parente.ontologie.RaisonneurService` | `List<String> classesPersonne(Long personneId)` ; `List<String> classesMariage(Long mariageId)` ; `boolean lienDeSangInfere(Long personne1Id, Long personne2Id)` ; `Coherence verifierCoherence()` |
| `bj.ifri.parente.ontologie.Coherence` | `record Coherence(boolean coherente, List<String> classesInsatisfiables)` |
| `bj.ifri.parente.ontologie.TurtleExporter` | `String exporter()` |
