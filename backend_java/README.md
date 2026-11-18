# API de détection des liens de parenté (Spring Boot + HermiT)

API JSON (sans interface) de la MVP « détection des liens de parenté ». Pour deux personnes, elle
répond à trois questions : sont-elles liées dans l'arbre, ont-elles un lien de sang, et
peuvent-elles se marier (droit béninois, loi n° 2002-07, art. 122, 123, 125 et 143).

Elle s'appuie sur l'ontologie famille (`src/main/resources/ontologie/famille.owl`, la TBox) et
sur l'arbre saisi par les utilisateurs (l'ABox, stockée en base H2). Les classes déduites
(Oncle, Cousine, DemiFrere, MariageDissousParDeces…), le lien `lienDeSangAvec` et la cohérence
sont **inférés par le raisonneur HermiT**, via la OWL API.

Le contrat HTTP est décrit dans `../SPEC_API.md` (sections 2 à 9) et dans `../SPEC_API_JAVA.md`,
qui est prioritaire.

## Prérequis

- **Java 25** (`java -version`).
- Pas d'autre installation : Maven est fourni par le wrapper `./mvnw`, et la base H2 est embarquée.

Pile technique : Spring Boot 4.1, Spring Data JPA, Bean Validation, H2, Jackson 3, OWL API 5.1.20 et
HermiT 1.4.5.519.

## Lancement

```bash
./mvnw spring-boot:run                                   # base vide
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo   # avec la famille de démonstration
```

L'API écoute sur `http://localhost:8080`. La base de développement est un fichier H2,
`./data/parente`. Supprimez `data/` pour repartir d'une base vide.

La date de référence (« aujourd'hui ») sert à l'âge, à la règle des 18 ans et au refus des
naissances futures. C'est la propriété `app.date-reference` (format `yyyy-MM-dd`) si elle est
définie, sinon la date du jour. Exemple :
`./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.date-reference=2026-11-18`.

### Données de démonstration (profil `demo`)

Si la base est vide au démarrage, le profil `demo` crée cette famille en passant par les services
(les mêmes règles que l'API s'appliquent) :

```
Grandpa ══ Grandma   (mariés en 1955, mariage dissous par décès en 2015)        Autre
    ├── Papa ══ Maman   (mariage en cours)            Maman + Autre ── Demi
    │     └── Kid                                     Papa beauParentDe Demi (famille recomposée)
    └── Tonton ══ Tantine   (divorcés en 2010)
          └── Cousin
```

Ids créés sur une base neuve : Grandpa 1, Grandma 2, Papa 3, Tonton 4, Maman 5, Autre 6,
Demi 7, Tantine 8, Kid 9, Cousin 10. Avec le lien `beauParentDe`, HermiT classe Demi en
`DemiFrere` et Kid en `DemiSoeur`.

## Routes

Toutes les routes sont sous `/api`, sans authentification. Le JSON est en snake_case et les dates
au format `yyyy-MM-dd`.

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/personnes` | Liste des personnes |
| POST | `/api/personnes` | Création (201), avec `doublons_probables` |
| GET | `/api/personnes/doublons` | Groupes de doublons probables (même nom et même date de naissance) |
| GET | `/api/personnes/{id}` | Détail : relations, frères et sœurs déduits, mariages et classes HermiT |
| PUT / PATCH | `/api/personnes/{id}` | Mise à jour partielle |
| DELETE | `/api/personnes/{id}` | Suppression (204), avec ses liens et ses mariages |
| GET / POST | `/api/liens/parente` | Liens `parentDe` (refus des cycles, d'un 3e parent…) |
| DELETE | `/api/liens/parente/{id}` | Suppression d'un lien `parentDe` |
| GET / POST | `/api/liens/beau-parente` | Liens `beauParentDe` |
| DELETE | `/api/liens/beau-parente/{id}` | Suppression d'un lien `beauParentDe` |
| GET / POST | `/api/mariages` | Mariages, avec leurs classes HermiT |
| GET / PATCH / DELETE | `/api/mariages/{id}` | Lecture, dissolution (`date_fin` et `cause_dissolution`), suppression |
| POST | `/api/verification` | Lien de sang, alliance, complétude et verdict sur le mariage |
| GET | `/api/arbre?personne1_id=..&personne2_id=..` | Graphe des deux personnes et chemin par l'ancêtre commun |
| GET | `/api/ontologie` | Signature de la TBox (classes, propriétés objet et de données) |
| GET | `/api/raisonneur/coherence` | Cohérence de TBox + ABox selon HermiT, classes insatisfiables |
| GET | `/api/export/turtle` | ABox assertée au format Turtle (`text/turtle`) |

Codes de retour :
- erreurs de validation et de règles métier : **422**, au format
  `{"message": "...", "errors": {"champ": ["..."]}}` ;
- ressource inexistante : **404** `{"message": "..."}` ;
- création : 201 ;
- suppression : 204.

### Exemple de vérification (profil demo)

```bash
curl -s -X POST localhost:8080/api/verification \
     -H 'Content-Type: application/json' \
     -d '{"personne1_id": 9, "personne2_id": 10}'
```

Résultat (extrait) :

```json
"lien_de_sang": { "existe": true, "type": "collaterale", "degre": 4, "code": "cousins_germains",
                  "ancetres_communs": [{"id": 1, "prenom": "Grandpa"}, {"id": 2, "prenom": "Grandma"}],
                  "message": "Lien de sang : cousins germains (4e degré)", "infere_par_raisonneur": true },
"mariage": { "verdict": "verification_incomplete", "regles": [{"code": "collaterale_eloignee", ...}] }
```

Le verdict est `verification_incomplete` parce que les parents de Maman et de Tantine ne sont pas
connus : l'application ne peut pas prouver l'absence d'un autre lien.

Autres appels utiles :

```bash
curl -s localhost:8080/api/personnes/7              # classes HermiT de Demi : DemiFrere, Frere…
curl -s -X POST localhost:8080/api/verification -H 'Content-Type: application/json' \
     -d '{"personne1_id": 5, "personne2_id": 4}'    # Maman et Tonton : beau-frère et belle-sœur, interdit
curl -s localhost:8080/api/raisonneur/coherence
curl -s localhost:8080/api/export/turtle
```

## Tests

```bash
./mvnw test          # 161 tests (profil test : H2 en mémoire, date de référence 2026-11-18)
./mvnw clean test    # en cas d'échec étrange (par exemple si un IDE compile en même temps dans target/)
```

Chaque test crée ses données par l'API (MockMvc) dans une transaction annulée à la fin.

## Exploitation de l'ontologie et du raisonneur

### Construction de l'ABox

À chaque requête qui en a besoin, l'application relit la base. Il n'y a pas de cache entre deux
requêtes. Elle construit ensuite une ontologie neuve, avec son propre `OWLOntologyManager`, en
quatre étapes :

1. `ProjectionABox` projette les données en assertions.
   - Chaque personne devient `f:personne_{id}`, typée `f:Personne` et `f:Masculin` ou
     `f:Feminin`. Elle porte `f:nom`, `f:prenom` (`xsd:string`), `f:dateNaissance`
     (`xsd:dateTime`) et `f:age` (`xsd:int`, calculé par l'application).
   - Les liens `f:parentDe` et `f:beauParentDe` sont repris tels quels.
   - `f:frereDe` et `f:soeurDe` relient deux personnes qui ont un parent commun.
   - Chaque mariage devient `f:mariage_{id}` (`f:Mariage`), avec `f:aPourConjoint` ×2,
     `f:dateDebut`, `f:dateFin`, `f:causeDissolution`, et `f:marieA` entre les conjoints.
2. `ConstructeurABox` copie les axiomes de la TBox (chargée une seule fois au démarrage, jamais
   modifiée) dans l'ontologie neuve. Il y ajoute les assertions et un axiome
   `DifferentIndividuals` sur tous les individus.
3. `RaisonneurService` crée un raisonneur HermiT
   (`org.semanticweb.HermiT.ReasonerFactory`) sur cette ontologie. Il l'enveloppe dans une
   `SessionRaisonnement`, ouverte une seule fois par requête et fermée par `dispose()`.
4. La même projection sert à l'export Turtle (`TurtleExporter`), qui ne contient donc que l'ABox
   assertée.

### Ce qu'infère HermiT

- **Les classes de chaque individu** sont obtenues par `reasoner.getTypes(individu, false)` :
  - on ne garde que les classes nommées de l'espace de noms `f:`, sans `owl:Thing` ;
  - les personnes reçoivent par exemple `Oncle`, `Cousine`, `PetiteFille`, `BelleFille`,
    `DemiFrere`, `Homme`, `Fillette` ou `Sexe` ;
  - les mariages reçoivent `MariageDissous`, `MariageDissousParDeces` ou
    `MariageDissousParDivorce`.

  Elles alimentent :
  - le détail d'une personne et ses mariages ;
  - les nœuds de l'arbre, calculés par un seul raisonneur avec `precomputeInferences` ;
  - les réponses de `/api/mariages`.

  Les résultats suivent la définition OWL, et non une règle codée à la main. Par exemple,
  `DemiFrere` exige un `BeauParent`, et `Sexe` vient de `Masculin ⊑ Sexe`.
- **`lienDeSangAvec`** : `infere_par_raisonneur` de `/api/verification` vaut
  `reasoner.isEntailed(lienDeSangAvec(x, y))`. Cette relation vient des axiomes
  `ancetreDe ⊑ lienDeSangAvec` et `descendantDe ∘ ancetreDe ⊑ lienDeSangAvec`. La paire x = y est
  exclue, car la chaîne rend la propriété réflexive en pratique.
- **La cohérence** : `/api/raisonneur/coherence` s'appuie sur `isConsistent()` et
  `getUnsatisfiableClasses()`. Si l'ABox est incohérente, les classes insatisfiables sont
  calculées sur la TBox seule.

### Ce qui reste dans le code (limites d'OWL décrites dans la MVP)

- **Le degré et le nom du lien.** OWL dit si deux personnes sont liées, pas à combien de
  générations. Le code fait un parcours en largeur sur les parents, cherche l'ancêtre commun le
  plus proche, calcule le degré selon la méthode civile, détecte les demi-frères et demi-sœurs
  (un seul parent commun) et nomme le lien. Le lien d'alliance, la complétude et les règles de
  mariage sont aussi calculés en Java.
- **`frereDe` et `soeurDe`.** OWL ne sait pas les déduire de parents communs : l'application les
  ajoute à l'ABox avant de lancer HermiT.
- **Les cycles.** Une propriété transitive comme `ancetreDe` ne peut pas être déclarée
  irréflexive. L'application refuse donc elle-même un `parentDe` qui créerait un cycle. Elle
  refuse aussi un troisième parent, un parent né après l'enfant, et un `parentDe` doublé d'un
  `beauParentDe` (propriétés disjointes).
- **Le mariage en cours.** En monde ouvert, l'absence de `dateFin` ne prouve rien.
  L'application considère donc qu'un mariage sans date de fin est en cours (art. 125 et 143).
- **L'absence de lien.** L'application répond « aucun lien trouvé dans les données connues » et
  indique la complétude de l'arbre. Le verdict « autorisé » n'est donné que si les deux arbres
  sont complets sur 2 générations.
