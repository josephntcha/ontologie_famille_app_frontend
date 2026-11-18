# Contrat de l'API Laravel – Détection des liens de parenté (MVP)

Ce document est **la source de vérité** partagée entre les tests et l'implémentation.
Sources : `MVP – Application de détection des liens de parenté.pdf` et `web_semantiq_famille_ex.owl` (TBox v2).

- Projet Laravel dans `backend/`. Back seul (API JSON), aucun front.
- Base : SQLite (tests : SQLite `:memory:` + `RefreshDatabase`).
- Tests : PHPUnit (`tests/Feature`, `tests/Unit`), toutes les classes de test étendent `Tests\TestCase`.
- Pas de raisonneur OWL en PHP : les classes déduites sont calculées par un moteur de règles PHP qui reproduit la TBox (section 7). L'ontologie est copiée dans `backend/resources/ontologie/famille.owl` et lue par `OntologieService`.
- Espace de noms de l'ontologie : `http://www.semanticweb.org/sunday/ontologies/2026/9/untitled-ontology-2#` (préfixe `f:`).
- Toutes les routes sont préfixées par `/api`, sans authentification. Réponses JSON (sauf l'export Turtle).
- Erreurs de validation et de règles métier : **HTTP 422**, format Laravel standard `{ "message": "...", "errors": { "<champ>": ["..."] } }`. Le champ à utiliser pour chaque règle est indiqué ci-dessous (les tests utilisent `assertJsonValidationErrors([...])`).
- Ressource inexistante : **404**.
- Dates en entrée et en sortie au format `Y-m-d`.

---

## 1. Modèles et tables (contrat utilisé directement par les tests)

Les tests créent des données soit par l'API, soit directement par `Model::create([...])`. Les attributs ci-dessous doivent donc être **fillable** avec exactement ces noms.

| Modèle | Table | Colonnes (fillable) |
|---|---|---|
| `App\Models\Personne` | `personnes` | `nom` (string), `prenom` (string), `sexe` (`'M'` ou `'F'`), `date_naissance` (date, cast `date`) |
| `App\Models\Filiation` (parentDe) | `filiations` | `parent_id`, `enfant_id` (FK personnes, cascade on delete) |
| `App\Models\BeauParente` (beauParentDe) | `beau_parentes` | `beau_parent_id`, `enfant_id` (FK personnes, cascade on delete) |
| `App\Models\Mariage` | `mariages` | `conjoint1_id`, `conjoint2_id` (FK personnes, cascade), `date_debut` (date), `date_fin` (date, nullable), `cause_dissolution` (`'deces'`, `'divorce'` ou null) |

`Personne` expose au minimum les relations `parents()`, `enfants()` (belongsToMany via `filiations`) et un accesseur `age` (âge entier en années révolues à `now()`, donc compatible avec `Carbon::setTestNow`).

`Mariage` : un mariage est **en cours** s'il n'a pas de `date_fin` (art. 125 et 143).

---

## 2. Format JSON d'une personne

```json
{ "id": 1, "nom": "Dupont", "prenom": "Alice", "sexe": "F", "date_naissance": "1990-05-12", "age": 36,
  "uri": "http://www.semanticweb.org/sunday/ontologies/2026/9/untitled-ontology-2#personne_1" }
```

---

## 3. Écran « Personnes »

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| GET | `/api/personnes` | – | 200 `{ "data": [Personne...] }` |
| POST | `/api/personnes` | `nom`, `prenom`, `sexe`, `date_naissance` (tous requis) | 201 `{ "data": Personne, "doublons_probables": [Personne...] }` |
| GET | `/api/personnes/doublons` | – | 200 `{ "data": [ { "nom": "...", "date_naissance": "Y-m-d", "personnes": [Personne...] } ] }` |
| GET | `/api/personnes/{id}` | – | 200 `{ "data": PersonneDetail }` |
| PUT/PATCH | `/api/personnes/{id}` | champs partiels | 200 `{ "data": Personne }` |
| DELETE | `/api/personnes/{id}` | – | 204 (supprime aussi ses filiations, beaux-liens et mariages) |

Validation (422) : `nom` et `prenom` chaînes non vides (max 255) ; `sexe` ∈ {`M`,`F`} ; `date_naissance` date valide, pas dans le futur.

**Doublons probables** : même `nom` (comparaison insensible à la casse et aux espaces de bord) **et** même `date_naissance`. La création n'est **pas** bloquée : la personne est créée (201) et `doublons_probables` liste les personnes existantes correspondantes (hors elle-même ; tableau vide sinon). `/api/personnes/doublons` renvoie uniquement les groupes d'au moins 2 personnes.

**PersonneDetail** = Personne + :
```json
{ "parents": [Personne], "enfants": [Personne], "beaux_parents": [Personne], "beaux_enfants": [Personne],
  "freres": [Personne], "soeurs": [Personne],
  "conjoints": [Personne], "mariages": [Mariage],
  "classes": ["Enfant", "Feminin", "Personne", "Soeur", ...] }
```
- `freres` / `soeurs` = **frereDe / soeurDe déduits** : autres personnes partageant au moins un parent (demi compris), séparées selon le sexe du frère/de la sœur (`M` → `freres`, `F` → `soeurs`).
- `conjoints` : tous les conjoints, mariages en cours ou dissous.
- `classes` : voir section 7.

---

## 4. Écran « Liens »

### 4.1 parentDe — `/api/liens/parente`
| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| GET | `/api/liens/parente` | – | 200 `{ "data": [ {id, parent_id, enfant_id} ] }` |
| POST | `/api/liens/parente` | `parent_id`, `enfant_id` | 201 `{ "data": {id, parent_id, enfant_id} }` |
| DELETE | `/api/liens/parente/{id}` | – | 204 |

Refus (422), avec la clé d'erreur indiquée :
| Règle | Clé |
|---|---|
| `parent_id` / `enfant_id` absent ou personne inexistante | le champ concerné |
| parent = enfant (parentDe irréflexive) | `enfant_id` |
| lien déjà existant | `enfant_id` |
| l'enfant a déjà 2 parents | `enfant_id` |
| **cycle** : `enfant_id` est déjà ancêtre (à toute profondeur) de `parent_id`, ce qui couvre aussi l'asymétrie | `parent_id` |
| le parent n'est pas né strictement avant l'enfant | `parent_id` |
| un lien beauParentDe existe déjà entre ces deux personnes, dans un sens ou l'autre (propriétés disjointes) | `parent_id` |

### 4.2 beauParentDe — `/api/liens/beau-parente`
| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| GET | `/api/liens/beau-parente` | – | 200 `{ "data": [ {id, beau_parent_id, enfant_id} ] }` |
| POST | `/api/liens/beau-parente` | `beau_parent_id`, `enfant_id` | 201 `{ "data": {id, beau_parent_id, enfant_id} }` |
| DELETE | `/api/liens/beau-parente/{id}` | – | 204 |

Refus (422) : champ absent ou inexistant (le champ) ; même personne (`enfant_id`) ; doublon (`enfant_id`) ; un parentDe existe déjà entre eux, dans un sens ou l'autre (`beau_parent_id`).

### 4.3 Mariages — `/api/mariages`
Format JSON d'un mariage :
```json
{ "id": 1, "conjoint1_id": 3, "conjoint2_id": 4, "date_debut": "2000-06-01", "date_fin": null,
  "cause_dissolution": null, "en_cours": true, "classes": ["Mariage"] }
```
`classes` (règles de la TBox) : toujours `Mariage` ; + `MariageDissous` si `date_fin` est renseignée ; + `MariageDissousParDeces` si la cause est `deces` ; + `MariageDissousParDivorce` si la cause est `divorce`. L'ordre dans le tableau n'a pas d'importance.

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| GET | `/api/mariages` | – | 200 `{ "data": [Mariage] }` |
| POST | `/api/mariages` | `conjoint1_id`, `conjoint2_id`, `date_debut` (requis), `date_fin`, `cause_dissolution` (optionnels) | 201 `{ "data": Mariage }` |
| GET | `/api/mariages/{id}` | – | 200 `{ "data": Mariage }` |
| PATCH | `/api/mariages/{id}` | `date_fin`, `cause_dissolution` (dissolution) | 200 `{ "data": Mariage }` |
| DELETE | `/api/mariages/{id}` | – | 204 |

Refus (422) :
| Règle | Clé |
|---|---|
| conjoint absent ou inexistant | le champ |
| `conjoint1_id` = `conjoint2_id` (marieA irréflexive) | `conjoint2_id` |
| `date_fin` antérieure à `date_debut` | `date_fin` |
| `cause_dissolution` hors {`deces`,`divorce`} | `cause_dissolution` |
| `date_fin` sans `cause_dissolution` ou `cause_dissolution` sans `date_fin` (les deux vont ensemble) | `cause_dissolution` |
| création d'un mariage **en cours** alors que `conjoint1` a déjà un mariage en cours | `conjoint1_id` |
| idem pour `conjoint2` (si conjoint1 est libre) | `conjoint2_id` |

Un mariage déjà dissous (avec `date_fin`) peut toujours être enregistré, pour saisir l'historique.

Chaque mariage produit, dans le modèle RDF, un individu `f:Mariage` (`f:aPourConjoint` ×2) et le lien symétrique `f:marieA` entre les conjoints (section 9).

---

## 5. Écran « Vérification » — `POST /api/verification`

Corps : `personne1_id`, `personne2_id` (requis, existants ; 422 `personne2_id` s'ils sont identiques).

Réponse 200 :
```json
{
  "personne1": Personne,
  "personne2": Personne,
  "lien_de_sang": {
    "existe": true,
    "type": "collaterale",
    "degre": 4,
    "generation_personne1": 2,
    "generation_personne2": 2,
    "code": "cousins_germains",
    "libelle": "Cousins germains",
    "demi": false,
    "ancetres_communs": [ {"id": 1, "nom": "...", "prenom": "..."} ],
    "message": "Lien de sang : cousins germains (4e degré)"
  },
  "lien_alliance": {
    "existe": false, "type": null, "degre": null, "code": null, "libelle": null,
    "conjoint_id": null, "mariage_id": null, "mariage_dissous_par_deces": false,
    "message": "Aucun lien d'alliance trouvé dans les données connues"
  },
  "mariage": {
    "verdict": "autorise",
    "regles": [ { "code": "collaterale_eloignee", "article": "Art. 122", "verdict": "autorise", "message": "..." } ],
    "avertissement": "Indication fournie à titre informatif, pas un avis juridique : adressez-vous à l'officier d'état civil."
  },
  "completude": {
    "personne1": { "parents_connus": 2, "parents_attendus": 2, "grands_parents_connus": 4, "grands_parents_attendus": 4, "generations_completes": 2, "complet": true },
    "personne2": { ... },
    "complet": true,
    "manquants": []
  }
}
```

### 5.1 Algorithme du lien de sang (méthode civile, MVP)
1. Ancêtres de x par parcours en largeur sur enfantDe, avec génération minimale (parents = 1, grands-parents = 2…). Idem pour y.
2. Si y est ancêtre de x (ou l'inverse) → `type = "ligne_directe"`, `degre` = génération de l'ancêtre ; la génération de l'ancêtre lui-même vaut 0 (ex. x enfant de y → `generation_personne1 = 1`, `generation_personne2 = 0`) ; `ancetres_communs` = [l'ancêtre (x ou y)].
3. Sinon, ancêtres communs ; on garde ceux qui minimisent g(x)+g(y) → `type = "collaterale"`, `degre` = ce minimum, `generation_personne1/2` = g(x), g(y) ; `ancetres_communs` = **tous** les ancêtres communs qui atteignent ce minimum (ex. les deux grands-parents pour des cousins germains).
4. Aucun ancêtre commun → `existe = false`, `type/degre/generation_*/code/libelle = null`, `ancetres_communs = []`, `demi = false`, `message = "Aucun lien de sang trouvé dans les données connues"`. (Ne jamais répondre « aucun lien » tout court : le message doit contenir « dans les données connues ».)
5. `demi = true` si g(x) = g(y) = 1 et que x et y n'ont **qu'un seul** parent commun.

### 5.2 Nommage (`code` / `libelle`), avec (a, b) = (min(g1,g2), max(g1,g2))
| (a, b) | code | libelle |
|---|---|---|
| (0, 1) | `parent_enfant` | Parent et enfant |
| (0, 2) | `grand_parent_petit_enfant` | Grand-parent et petit-enfant |
| (0, n≥3) | `ascendant_descendant` | Ascendant et descendant (ligne directe) |
| (1, 1) non demi | `freres_soeurs` | Frère et sœur |
| (1, 1) demi | `demi_freres_soeurs` | Demi-frère et demi-sœur |
| (1, 2) | `oncle_neveu` | Oncle ou tante et neveu ou nièce |
| (2, 2) | `cousins_germains` | Cousins germains |
| (1, 3) | `grand_oncle_petit_neveu` | Grand-oncle et petit-neveu |
| (3, 3) | `cousins_issus_de_germains` | Cousins issus de germains |
| autre collatéral | `collateral_eloigne` | Parents collatéraux |

### 5.3 Lien d'alliance (étape 5 du MVP)
Pour chaque mariage (en cours **ou** dissous) de x avec un conjoint c (c ≠ y), on calcule le lien de sang entre c et y ; de même pour chaque conjoint c de y (c ≠ x) avec x. On ajoute aussi les liens **beauParentDe** déclarés entre x et y (dans un sens ou l'autre), qui donnent une alliance en ligne directe de degré 1 (`conjoint_id` et `mariage_id` null).

Codes d'alliance (le degré est celui du lien de sang trouvé avec le conjoint) :
| Cas | type | code |
|---|---|---|
| ligne directe, degré 1 (parent ou enfant du conjoint, ou beauParentDe) | `ligne_directe` | `beau_parent_bel_enfant` |
| ligne directe, degré ≥ 2 | `ligne_directe` | `ascendant_descendant_par_alliance` |
| collatéral degré 2 (frère/sœur du conjoint) | `collaterale` | `beau_frere_belle_soeur` |
| collatéral degré 3 | `collaterale` | `oncle_neveu_par_alliance` |
| collatéral degré ≥ 4 | `collaterale` | `allie_eloigne` |

S'il y a plusieurs alliances, on retient la plus restrictive : ligne directe avant collatérale, puis le plus petit degré, puis un mariage **non** dissous par décès avant un mariage dissous par décès.
`mariage_dissous_par_deces` = true si le mariage qui crée l'alliance retenue a la cause `deces`.
Aucune alliance → `existe = false`, champs à null, `message = "Aucun lien d'alliance trouvé dans les données connues"`.

### 5.4 Règles de mariage (droit béninois, loi n° 2002-07)
Chaque règle déclenchée ajoute une entrée dans `mariage.regles` : `{code, article, verdict, message}`.

| code | Condition | verdict de la règle | article |
|---|---|---|---|
| `ligne_directe_sang` | lien de sang en ligne directe (tout degré) | `interdit` | `Art. 122` |
| `ligne_directe_alliance` | alliance en ligne directe | `interdit` | `Art. 122` |
| `collaterale_sang` | lien de sang collatéral de degré ≤ 3 (demi compris) | `interdit` | `Art. 122` |
| `collaterale_alliance` | alliance collatérale de degré ≤ 3, sauf l'exception ci-dessous | `interdit` | `Art. 122` |
| `beau_frere_belle_soeur_deces` | alliance `beau_frere_belle_soeur` dont le mariage est dissous **par décès** | `autorisation_requise` (autorisation du procureur pour motif grave) | `Art. 122` |
| `collaterale_eloignee` | lien de sang collatéral de degré ≥ 4 | `autorise` | `Art. 122` |
| `age_minimum` | l'une des deux personnes a moins de 18 ans aujourd'hui | `interdit` (sauf dispense d'âge du tribunal) | `Art. 123` |
| `mariage_en_cours` | l'une des deux personnes a un mariage sans `date_fin` | `interdit` | `Art. 125 et 143` |
| `aucun_empechement` | ajoutée seulement si aucune autre règle n'est déclenchée | `autorise` | `Art. 122` |

Un mariage dissous par **divorce** garde l'interdiction d'alliance (pas d'exception).

**Verdict global** (`mariage.verdict`), par ordre de priorité :
1. `interdit` si au moins une règle est `interdit` (un empêchement trouvé est certain, même si l'arbre est incomplet) ;
2. sinon `autorisation_requise` si une règle l'est ;
3. sinon `verification_incomplete` si `completude.complet` est faux ;
4. sinon `autorise`.

### 5.5 Complétude
Pour chaque personne : `parents_connus` (0–2) sur `parents_attendus = 2`, `grands_parents_connus` (0–4, parents des parents connus) sur `grands_parents_attendus = 4`.
`generations_completes` = 2 si 2/2 et 4/4 ; 1 si seulement 2/2 ; 0 sinon. `complet` = (generations_completes == 2).
`completude.complet` = les deux personnes sont complètes.
`manquants` = une entrée `{ "personne_id": id, "description": "..." }` par personne dont un parent est inconnu (personne_id = la personne elle-même) et par parent connu dont un parent est inconnu (personne_id = ce parent). Liste vide si tout est complet.

---

## 6. Écran « Arbre » — `GET /api/arbre?personne1_id=..&personne2_id=..`
422 si un id manque ou n'existe pas.
```json
{
  "noeuds": [ { "id": 1, "nom": "...", "prenom": "...", "sexe": "M", "classes": ["..."] } ],
  "aretes": [ { "source": 1, "cible": 2, "type": "parentDe" } ],
  "chemin": [5, 3, 1, 4, 6],
  "ancetre_commun_id": 1
}
```
- `noeuds` : pour chacune des deux personnes, elle-même, tous ses ancêtres, tous ses descendants et ses conjoints ; plus toutes les personnes du chemin. Pas de doublon.
- `aretes` : tous les liens entre deux nœuds présents ; `type` ∈ {`parentDe` (source = parent), `beauParentDe` (source = beau-parent), `marieA` (source = conjoint1)}.
- `chemin` : liste d'ids de personne1 jusqu'à personne2 en passant par l'ancêtre commun le plus proche (en ligne directe : de l'un à l'autre). En cas d'égalité entre ancêtres, n'importe lequel convient. Vide (`[]`) et `ancetre_commun_id = null` si pas de lien de sang.

---

## 7. Classes déduites (moteur de règles PHP qui reproduit la TBox)

`App\Services\ClassificationService::classes(Personne $p): array` renvoie un tableau de noms locaux de classes, **trié par ordre alphabétique, sans doublon**. Règles (marieA = au moins un mariage, en cours ou dissous) :

| Classe | Règle |
|---|---|
| `Personne` | toujours |
| `Masculin` / `Feminin` | selon `sexe` |
| `Parent` | a au moins un enfant (parentDe) |
| `Pere` / `Mere` | Parent et Masculin / Feminin |
| `GrandParent` | parentDe d'une personne qui est Parent |
| `GrandPere` / `GrandMere` | GrandParent et Masculin / Feminin |
| `Enfant` | a au moins un parent connu |
| `Fils` / `Fille` | Enfant et Masculin / Feminin |
| `PetitEnfant` | enfant d'une personne qui est Enfant |
| `PetitFils` / `PetiteFille` | PetitEnfant et Masculin / Feminin |
| `Frere` / `Soeur` | Masculin / Feminin, et frereDe / soeurDe au moins une personne (parent commun déduit) |
| `DemiFrere` / `DemiSoeur` | Frere / Soeur et partage exactement un parent avec au moins un de ses frères et sœurs |
| `Oncle` / `Tante` | Masculin / Feminin, et frère / sœur d'une personne qui est Parent |
| `Neveu` / `Niece` | Masculin / Feminin, et enfant d'un Frere ou d'une Soeur |
| `Cousin` / `Cousine` | Masculin / Feminin, et enfant d'un Oncle ou d'une Tante |
| `BeauParent` | a au moins un lien beauParentDe sortant |
| `BeauPere` / `BelleMere` | BeauParent et Masculin / Feminin |
| `BeauFils` | Masculin et marieA une personne de classe Fille |
| `BelleFille` | Feminin et marieA une personne de classe Fils |
| `Homme` / `Femme` | Masculin / Feminin et âge ≥ 21 |
| `Garcon` / `Fillette` | Masculin / Feminin et âge ≤ 20 |

---

## 8. Ontologie — `GET /api/ontologie`
`App\Services\OntologieService` lit `resources/ontologie/famille.owl` (RDF/XML, avec DOM ou SimpleXML) :
```json
{ "namespace": "http://www.semanticweb.org/sunday/ontologies/2026/9/untitled-ontology-2#",
  "classes": ["BeauFils", "BeauParent", ...],
  "proprietes_objet": ["aPourConjoint", "ancetreDe", ...],
  "proprietes_donnees": ["age", "causeDissolution", ...] }
```
Noms locaux, triés alphabétiquement, uniquement les entités **déclarées** avec `rdf:about` (pas les classes anonymes). Méthodes : `namespace(): string`, `classes(): array`, `proprietesObjet(): array`, `proprietesDonnees(): array`.

## 9. Export RDF de l'ABox — `GET /api/export/turtle`
200, `Content-Type: text/turtle` (charset accepté). `App\Services\TurtleExporter::exporter(): string`.
- Préfixes : `@prefix f: <namespace> .`, `rdf`, `rdfs`, `xsd`, `owl`.
- Chaque personne : `f:personne_{id} a owl:NamedIndividual, f:Personne, f:Masculin|f:Feminin ; f:nom "..." ; f:prenom "..." ; f:dateNaissance "YYYY-MM-DDT00:00:00"^^xsd:dateTime ; f:age "N"^^xsd:int .`
- Liens : `f:parentDe`, `f:beauParentDe`, `f:frereDe` / `f:soeurDe` (déduits par l'application avant raisonnement), `f:marieA` (dans les deux sens).
- Chaque mariage : `f:mariage_{id} a owl:NamedIndividual, f:Mariage ; f:aPourConjoint f:personne_a, f:personne_b ; f:dateDebut "..."^^xsd:dateTime` (+ `f:dateFin`, `f:causeDissolution "deces"|"divorce"` si renseignés).
- Les chaînes sont échappées (guillemets, antislash).
- Tous les termes `f:` utilisés (hors `personne_*` et `mariage_*`) doivent exister dans l'ontologie.

---

## 10. Services (contrat des tests unitaires)

Tous résolus par le conteneur (`app(Classe::class)`), sans argument de constructeur obligatoire hors injection.

| Classe | Méthode | Retour |
|---|---|---|
| `App\Services\NommageLien` | `nommer(int $g1, int $g2, bool $demi = false): array` | `['code' => ..., 'libelle' => ..., 'degre' => g1+g2, 'type' => 'ligne_directe'\|'collaterale']` selon 5.2 |
| `App\Services\GenealogieService` | `ancetres(Personne $p): array` | `[ancetreId => generationMin]` (sans p) |
| | `descendants(Personne $p): array` | `[descendantId => generationMin]` (sans p) |
| | `creeraitUnCycle(int $parentId, int $enfantId): bool` | vrai si enfant = parent ou enfant est ancêtre de parent |
| | `freresEtSoeurs(Personne $p): \Illuminate\Support\Collection` | Personnes partageant ≥ 1 parent (sans p) |
| | `lienDeSang(Personne $x, Personne $y): array` | structure `lien_de_sang` de 5 **mais** `ancetres_communs` = tableau d'**ids** (int) |
| `App\Services\AllianceService` | `lienAlliance(Personne $x, Personne $y): array` | structure `lien_alliance` de 5 |
| `App\Services\CompletudeService` | `evaluer(Personne $p): array` | structure `completude.personne1` de 5.5 |
| `App\Services\ReglesMariageService` | `evaluer(Personne $x, Personne $y, array $lienDeSang, array $lienAlliance, bool $complet): array` | structure `mariage` de 5 (`verdict`, `regles`, `avertissement`) |
| `App\Services\VerificationService` | `verifier(Personne $x, Personne $y): array` | réponse complète de 5 |
| `App\Services\ClassificationService` | `classes(Personne $p): array` | section 7 |
| `App\Services\OntologieService` | section 8 | |
| `App\Services\TurtleExporter` | `exporter(): string` | section 9 |
