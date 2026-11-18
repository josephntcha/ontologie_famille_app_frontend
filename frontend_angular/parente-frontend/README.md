# Registre de parenté — frontend Angular

Interface web de l'application de détection des liens de parenté (MVP). Elle communique avec le
backend Spring Boot + HermiT (`backend_java/`). Toute la logique (lien de sang, alliance, règles
de mariage, raisonnement OWL) reste dans le backend : le frontend saisit les données et affiche les
résultats.

## 1. Prérequis

| Outil | Version | Pour | Vérifier |
|---|---|---|---|
| JDK | **25** | backend | `java -version` |
| Node.js | **22.22+** ou **24.15+** | frontend | `node -v` |
| npm | fourni avec Node | frontend | `npm -v` |

Maven n'est pas à installer : le backend fournit le wrapper `./mvnw`. Les commandes Angular passent
par `npm` : inutile d'installer `@angular/cli` globalement.

Organisation conseillée des dossiers :

```
ontologie_famille_app/
├── backend_java/        ← le backend fourni (Spring Boot, port 8080)
└── parente-frontend/    ← ce projet (Angular, port 4200)
```

## 2. Démarrer le backend (terminal 1)

```bash
cd backend_java
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo     # Linux / macOS
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo   # Windows
```

- Le premier lancement télécharge les dépendances Maven (quelques minutes).
- L'API est prête quand le journal affiche `Started ParenteApplication`. Test rapide :
  `curl http://localhost:8080/api/personnes`.
- Le profil `demo` crée une famille d'exemple (Grandpa, Grandma, Papa, Maman, Kid, Cousin, Demi…)
  **seulement si la base est vide**. Sans `-Dspring-boot.run.profiles=demo`, la base démarre vide.
- La base H2 est le fichier `backend_java/data/parente`. Supprimez le dossier `data/` pour repartir
  de zéro.

## 3. Démarrer le frontend (terminal 2)

```bash
cd parente-frontend
npm install        # la première fois seulement
npm start          # équivaut à « ng serve »
```

Ouvrez **http://localhost:4200**. Le voyant en bas du menu passe au vert (« API connectée, ontologie
cohérente ») quand le backend répond.

Pas de CORS à configurer en développement : `ng serve` relaie tout appel `/api/...` vers
`http://localhost:8080` grâce à `proxy.conf.json`. Pour le navigateur, front et API ont la même
origine.

## 4. Premier parcours avec la famille de démonstration

| Vérifier… | Résultat attendu |
|---|---|
| Kid et Cousin | Cousins germains (4e degré) ; « Vérification incomplète », car les parents de Maman et de Tantine ne sont pas saisis |
| Maman et Tonton | Beau-frère et belle-sœur (alliance, 2e degré) ; « Mariage interdit » |
| Kid et Demi | Demi-frère et demi-sœur (un seul parent commun) ; « Mariage interdit » |
| Kid et Grandpa | Ligne directe, 2e degré ; « Mariage interdit » |

Puis ouvrez **Arbre** avec Kid et Cousin : le chemin Kid → Papa → Grandpa → Tonton → Cousin est
surligné ; un clic sur Demi affiche ses classes HermiT (`DemiFrere`, `Frere`, `Fils`…).

## 5. Comment ça marche

### Architecture

```
Navigateur ── http://localhost:4200 ──► ng serve (Angular)
                                             │  /api/*  (proxy.conf.json)
                                             ▼
                                   Spring Boot :8080  ──► H2 : personnes, filiations, mariages (ABox)
                                             │
                                             └──► OWL API + HermiT : famille.owl (TBox) + ABox
                                                  → classes inférées, lienDeSangAvec, cohérence
```

À chaque requête qui en a besoin, le backend relit la base, construit l'ABox (avec `frereDe` /
`soeurDe` déduits des parents communs), y ajoute la TBox et lance HermiT. Le degré, le nom du lien,
l'alliance, la complétude et les règles du Code des personnes et de la famille sont calculés en Java.

### Les écrans et les routes qu'ils appellent

| Écran | Ce que fait l'utilisateur | Routes API |
|---|---|---|
| Accueil | Choisit deux personnes ; voit l'état du registre | `GET /api/personnes`, `/liens/parente`, `/liens/beau-parente`, `/mariages`, `/personnes/doublons`, `/raisonneur/coherence` |
| Personnes | Ajoute une personne ; repère les doublons probables | `GET/POST /api/personnes`, `GET /api/personnes/doublons` |
| Fiche personne | Consulte relations et classes HermiT ; modifie ; supprime | `GET/PATCH/DELETE /api/personnes/{id}` |
| Liens › Filiation | Déclare ou supprime `parentDe` | `GET/POST /api/liens/parente`, `DELETE /api/liens/parente/{id}` |
| Liens › Beau-parent | Déclare ou supprime `beauParentDe` | `GET/POST /api/liens/beau-parente`, `DELETE …/{id}` |
| Liens › Mariage | Enregistre, dissout (décès ou divorce), supprime | `GET/POST /api/mariages`, `PATCH/DELETE /api/mariages/{id}` |
| Vérification | Lance la vérification de deux personnes | `POST /api/verification`, `GET /api/arbre` (pour nommer le chemin) |
| Arbre | Affiche le graphe et le chemin surligné | `GET /api/arbre?personne1_id=…&personne2_id=…` |
| Ontologie | Consulte la TBox, relance HermiT, exporte l'ABox | `GET /api/ontologie`, `/raisonneur/coherence`, `/export/turtle` |

### L'écran Vérification en détail

1. **Verdict** : `mariage.verdict` (interdit, autorisation requise, vérification incomplète,
   autorisé) et chaque règle déclenchée avec son article (122, 123, 125 et 143).
2. **Échelle des degrés** : la remontée de chaque personne jusqu'à l'ancêtre commun, génération par
   génération, et le calcul civil `g(x) + g(y) = degré`. Le chemin nominatif vient de `/api/arbre`.
3. **Lien d'alliance** : par quel conjoint et quel mariage, et si ce mariage a été dissous par décès.
4. **Fiabilité de l'arbre** : parents connus sur 2, grands-parents sur 4, et la liste des personnes
   à compléter. Monde ouvert oblige, l'application n'affirme jamais l'absence de lien.
5. **HermiT** : indique si le raisonneur infère `lienDeSangAvec(x, y)` et signale un écart avec le
   calcul du degré.

### Erreurs

- **422** (validation, règle métier) : chaque message s'affiche sous le champ concerné (`parent_id`,
  `conjoint1_id`, `date_naissance`…), tel que le backend l'a rédigé.
- **404** : message « introuvable » sur la page.
- **Backend arrêté** : notification et voyant rouge dans le menu, avec un bouton « Réessayer ».

### Organisation du code

```
src/app/
├── core/                 couche d'accès à l'API, sans affichage
│   ├── models.ts         interfaces TypeScript du contrat JSON (snake_case, comme l'API)
│   ├── api.service.ts    un appel par route (ParenteApi)
│   ├── api-error.ts      normalisation des erreurs 422 / 404 / 409 / réseau
│   ├── personnes.store.ts liste des personnes partagée (signals)
│   ├── libelles.ts       textes des verdicts, degrés, classes OWL lisibles
│   └── http-error.interceptor.ts, toast.service.ts
├── shared/               sélecteur de personne, erreurs de champ, classes OWL, notifications
└── features/             un dossier par écran (chargé à la demande)
    ├── accueil/  personnes/  liens/  verification/  arbre/  ontologie/
```

Choix techniques : Angular 22 (composants standalone, signals, sans zone.js), Reactive Forms,
routes chargées à la demande, Cytoscape.js + dagre pour l'arbre (carré = homme, cercle = femme,
convention généalogique), polices Public Sans et Newsreader embarquées (pas de CDN).

## 6. Construire pour la production

```bash
npm run build     # sortie : dist/parente-frontend/browser/
```

Le dossier produit est un site statique. Deux façons de le servir :

**Même domaine que l'API (recommandé)** : un serveur web sert les fichiers et relaie `/api` vers
Spring Boot. Exemple nginx :

```nginx
location /api/ { proxy_pass http://localhost:8080; }
location /     { root /chemin/vers/browser; try_files $uri $uri/ /index.html; }
```

**Domaines différents** : indiquez l'URL de l'API dans `src/app/app.config.ts`…

```ts
{ provide: API_BASE_URL, useValue: 'https://api.mon-domaine.bj/api' },
```

…et autorisez l'origine du frontend côté backend (fichier à ajouter dans `bj.ifri.parente.config`) :

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://mon-domaine.bj")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
```

## 7. Tests

```bash
npm test                 # frontend : Vitest (libellés, gestion des erreurs API)
cd ../backend_java && ./mvnw test   # backend : 161 tests
```

## 8. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| « API injoignable sur le port 8080 » | Backend non démarré ou encore en cours de démarrage | Lancez `./mvnw spring-boot:run`, attendez `Started ParenteApplication`, puis « Réessayer » |
| `release version 25 not supported` au build du backend | JDK trop ancien | Installez un JDK 25 et pointez `JAVA_HOME` dessus |
| `The Angular CLI requires a minimum Node.js version…` | Node trop ancien | Installez Node 22.22+ ou 24.15+ |
| Port 4200 déjà utilisé | Autre serveur lancé | `npm start -- --port 4300` |
| Aucune donnée de démonstration | La base n'était pas vide | Arrêtez le backend, supprimez `backend_java/data/`, relancez avec le profil `demo` |
| Réponse 409 | Les données rendent l'ontologie incohérente pour HermiT | Ouvrez l'écran Ontologie, puis corrigez le dernier lien saisi |
| Le champ date affiche `mm/dd/yyyy` | Langue du navigateur | Sans effet sur l'API ; un navigateur en français affiche `jj/mm/aaaa` |
