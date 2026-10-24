# Inventa API

API Spring Boot pour l’application mobile Inventa. Les utilisateurs sont authentifiés en HTTP Basic,
les mots de passe sont hachés en BCrypt et chaque endpoint métier contrôle le rôle côté serveur.

## Démarrage local

Sans profil spécifié (y compris avec le bouton Run habituel d’IntelliJ), l’application démarre
en mode local sans MySQL. Elle utilise une base H2 persistante dans `inventa-local.mv.db`; au
premier démarrage, l’écran Flutter propose de créer le compte directeur si aucun directeur
n’existe. Vous pouvez aussi sélectionner la configuration IntelliJ **Inventa local** ou lancer
explicitement le profil local :

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Pour utiliser MySQL, activez le profil `mysql` et configurez les secrets suivants sans les
enregistrer dans le dépôt :

```powershell
$env:INVENTA_DB_URL = 'jdbc:mysql://localhost:3306/inventa?createDatabaseIfNotExist=true&serverTimezone=UTC'
$env:INVENTA_DB_USERNAME = 'inventa'
$env:INVENTA_DB_PASSWORD = '<mot-de-passe-mysql>'
$env:INVENTA_DIRECTOR_USERNAME = 'directeur'
$env:INVENTA_DIRECTOR_PASSWORD = '<mot-de-passe-long-de-12-caracteres-minimum>'
$env:INVENTA_ASSISTANT_USERNAME = 'assistant'
$env:INVENTA_ASSISTANT_PASSWORD = '<mot-de-passe-long-de-12-caracteres-minimum>'
$env:INVENTA_BOOTSTRAP_ASSISTANT_PHONE_NUMBER = '+2250102030405'
$env:INVENTA_FIELD_MANAGER_USERNAME = 'terrain'
$env:INVENTA_FIELD_MANAGER_PASSWORD = '<mot-de-passe-long-de-12-caracteres-minimum>'
$env:INVENTA_BOOTSTRAP_FIELD_MANAGER_PHONE_NUMBER = '+2250102030406'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Les comptes fournis par variable d’environnement sont créés au premier démarrage et conservés en
base. Les secrets ne sont pas remplacés pour un compte existant. Aucun mot de passe par défaut
n’est fourni. Le numéro est obligatoire pour les comptes assistant et terrain créés ainsi.

Pour tester la page d’inscription du directeur, utilisez une base de test vide et désactivez le
compte directeur bootstrap (ne faites pas cet essai sur une base contenant vos données) :

```powershell
$env:INVENTA_DIRECTOR_USERNAME = ''
$env:INVENTA_DIRECTOR_PASSWORD = ''
```

Comme le formulaire n’est proposé que lorsqu’aucun directeur n’existe, une base déjà configurée
avec un directeur n’affichera pas ce bouton. L’inscription publique se ferme automatiquement
après la création du premier directeur; les soumissions simultanées sont sérialisées côté base.
Cette procédure premier démarrage est prévue pour le test et l’installation initiale, pas pour
remplacer l’authentification HTTPS ni être réactivée sur une installation déjà configurée.

Vérifiez l’API avec `GET http://localhost:8006/api/health`. Les autres routes exigent
l’authentification.

## API consommée par Flutter

- `GET /api/auth/me` (renvoie également le `fieldId` du responsable terrain)
- `GET /api/auth/director-registration`, `POST /api/auth/register-director`
- `GET /api/workers`, `POST /api/workers`, `PUT /api/workers/{id}/salary`,
  `PUT /api/workers/{id}/active`
- `GET /api/attendance?date=YYYY-MM-DD`, `GET /api/attendance/month?month=YYYY-MM`,
  `PUT /api/attendance/{workerId}?date=YYYY-MM-DD`
- `GET /api/tasks`, `POST /api/tasks`, `PUT /api/tasks/{id}/status`
- `GET /api/stock/items`, `POST /api/stock/items`
- `PUT /api/stock/items/{id}/price`, `GET /api/stock/items/{id}/price-history`
- `GET /api/stock/orders`, `POST /api/stock/orders`,
  `PUT /api/stock/orders/{id}/send`, `PUT /api/stock/orders/{id}/receive`
- `GET /api/stock/movements`, `POST /api/stock/movements`
- `GET /api/stock/alerts`, `POST /api/stock/alerts`,
  `PUT /api/stock/alerts/{id}/resolve`
- `GET /api/expenses`, `POST /api/expenses`
- `GET /api/budgets` (directeur/assistant), `GET /api/budgets/field` (responsable, son champ uniquement),
  `PUT /api/budgets/limit`, `POST /api/budgets/funding` (ajout de fonds par le directeur),
  `POST /api/budgets/allocations`, `POST /api/expenses/field-budget` (dépense motivée du responsable)
- `POST /api/expenses/offline-sync` (assistant uniquement)
- `GET /api/sales`, `POST /api/sales`
- `GET /api/payments`, `GET /api/payments/monthly?month=YYYY-MM`, `POST /api/payments`
- `GET /api/reports/monthly?month=YYYY-MM` (présences, tâches, stocks, alertes, dépenses, paiements,
  ventes, heures supplémentaires; les activités quotidiennes du directeur ne comprennent que les
  journées validées, les paiements sont listés indépendamment)
- `POST /api/payroll/workers/{workerId}/payments?through=YYYY-MM` (paiement direct par l’assistant)
- `POST /api/push/devices`, `DELETE /api/push/devices`
- `GET /api/payroll/settings`, `PUT /api/payroll/settings`
- `GET /api/overtime`, `POST /api/overtime`
- `GET /api/fields`, `POST /api/fields`, `PUT /api/fields/{id}`
- `GET /api/accounts`, `POST /api/accounts`, `PUT /api/accounts/{id}/enabled`,
  `PUT /api/accounts/{id}/field`
- `POST /api/push/devices`, `DELETE /api/push/devices`

Les dates de saisie et l’utilisateur responsable sont enregistrés par le serveur. Les sorties de
stock ne peuvent pas rendre une quantité négative. Les montants et quantités sont positifs.
Le téléphone est obligatoire lors de la création d’un ouvrier, d’un assistant ou d’un responsable
terrain; les réponses des ouvriers et comptes l’incluent. Une livraison signalée par le terrain
n’ajoute rien au stock tant qu’un assistant ne l’a pas validée. La validation, protégée contre les
doublons, ajoute la quantité et crée un mouvement de réception. La configuration de paie (salaire mensuel de référence et heures mensuelles complètes) est partagée
et persistée côté serveur. Une modification du salaire de référence met à jour les ouvriers sans
ajustement individuel; une modification de salaire par le directeur ou l’assistant est conservée
comme exception individuelle. Les ventes de récolte enregistrent le produit, la quantité, le prix
total, la date, le déclarant et le champ; elles sont incluses dans le rapport quotidien après
validation par l’assistant.

Le directeur fixe l’enveloppe de fonctionnement globale; seul l’assistant répartit les fonds entre
les champs et enregistre des dépenses budgétées. Le directeur et l’assistant voient les soldes et
les rapprochements globaux; chaque responsable ne voit que le solde attribué à son champ et enregistre
des dépenses motivées contre ce solde. Les dépenses assistant
hors ligne sont conservées avec une clé d’idempotence; si le solde disponible ne permet pas leur
validation à la synchronisation, elles restent en rapprochement jusqu’à ce que le budget le permette.
L’assistant peut ajuster à la hausse ou à la baisse la quantité demandée avant l’envoi. Il définit le
prix unitaire des articles. Au dispatch d’une demande, le serveur fige le prix,
la quantité, le total calculé et la date, indépendamment des modifications ultérieures du prix.
Les ouvriers, tâches, articles de stock et dépenses sont rattachés à un champ. Les présences,
paiements, heures supplémentaires et mouvements de stock sont rattachés au champ de l’ouvrier ou
de l’article correspondant. Les réponses de données incluent le `fieldId` pour les rapports.

Le directeur crée et modifie les champs avec un nom, un emplacement et une description facultative.
Lorsqu’il crée un compte `FIELD_MANAGER`, `fieldId` est obligatoire; `PUT /api/accounts/{id}/field`
permet de réaffecter un responsable à un autre champ. Plusieurs comptes responsables peuvent
partager un champ, mais un compte responsable ne peut avoir qu’un seul champ. Le compte
`ASSISTANT` reste central et n’est lié à aucun champ. Pour enregistrer un article de stock ou une
dépense au nom de l’assistant, envoyer le `fieldId` ciblé; si ce champ est omis, le backend choisit
le premier champ (le champ historique par défaut).

Au premier démarrage après cette mise à jour, l’initialisation crée un champ « Parcelle principale »
si aucun n’existe. Les anciens responsables sans champ et les anciennes lignes de travailleurs,
stocks, tâches et dépenses dont le champ est vide sont rattachés à ce champ. Les présences,
paiements, heures supplémentaires et mouvements héritent du champ de leur ouvrier ou article. Cette
initialisation conserve les comptes et les données existantes; elle ne supprime ni ne recrée les
tables. La stratégie Hibernate `update` ajoute les relations manquantes.
Le montant estimé dans l’écran de paie correspond au salaire mensuel effectif de chaque ouvrier
(référence commune ou exception individuelle), proratisé selon les heures de présence terminées
(arrivée et départ enregistrés) par rapport aux heures complètes
configurées pour le mois. Le directeur configure ces deux valeurs; elles sont consultables par les
autres rôles. Les heures supplémentaires restent séparées et ne sont pas incluses dans cette
estimation; les paiements réels restent saisis explicitement par l’assistant.
Dans l’écran de paie, l’assistant enregistre directement le cumul dû avec le bouton « Payer » de
chaque ouvrier; le serveur mémorise le montant, la période couverte, la date et l’auteur. Le registre
mensuel est accessible au directeur et à l’assistant.

## Rôles

- **DIRECTOR** : crée et modifie les champs, recrute/suspend les comptes et affecte les responsables;
  peut consulter toutes les données de tous les champs, notamment ouvriers, paie, présences,
  stocks, dépenses, paiements et heures supplémentaires. La création des comptes assistant
  et terrain exige un mot de passe initial d’au moins 12 caractères; aucun mot de passe n’est
  renvoyé par l’API.
- **ASSISTANT** : vue d’ensemble, ouvriers, présences, tâches, stocks/achats, dépenses, rapport
  et paie; peut valider les livraisons, enregistrer des achats et dépenses, consulter la
  configuration de paie et payer les ouvriers. Son compte central peut consulter les opérations de
  tous les champs, mais ne peut pas modifier les présences ou les tâches.
- **FIELD_MANAGER** : vue d’ensemble, ouvriers, présences, tâches, stocks, dépenses et paie;
  peut gérer les ouvriers, pointer l’équipe, suivre les tâches, utiliser le stock et déclarer les
  heures supplémentaires de son champ uniquement. Les paiements restent réservés au directeur et
  à l’assistant. Les enregistrements des autres champs sont masqués et ne peuvent pas être modifiés.

Les restrictions de rôle et le cloisonnement par champ sont appliqués côté API. Le directeur et
l’assistant ont une visibilité inter-champs pour leurs rapports; un responsable terrain est
toujours limité au champ rattaché à son compte.

## Configuration

`INVENTA_DB_URL`, `INVENTA_DB_USERNAME` et `INVENTA_DB_PASSWORD` configurent MySQL.
`INVENTA_BOOTSTRAP_ASSISTANT_PHONE_NUMBER` et
`INVENTA_BOOTSTRAP_FIELD_MANAGER_PHONE_NUMBER` sont requis si les
comptes bootstrap correspondants sont activés.
`INVENTA_JPA_DDL_AUTO` configure la stratégie de schéma (valeur locale par défaut : `update`).
Conservez une stratégie additive pour le déploiement de cette mise à jour de données; les colonnes
de rattachement sont remplies sans effacer les enregistrements existants.
Le projet utilise le dialecte Hibernate MySQL avec le pilote MySQL, y compris lorsqu’il se connecte
à MariaDB : cela évite le SQL `ALTER TABLE ... IF EXISTS` non pris en charge par les anciennes
versions de MariaDB. Au redémarrage avec `ddl-auto=update`, Hibernate ajoute notamment la colonne
`workers.salary_is_individual` et la table `harvest_sales` sans recréer les tables existantes.
`INVENTA_ERROR_INCLUDE_MESSAGE` contrôle l’exposition des messages d’erreur (par défaut `never`).
`INVENTA_CORS_ALLOWED_ORIGINS` configure les origines web autorisées (séparées par des virgules);
la valeur locale par défaut n’autorise que localhost. Le port HTTP par défaut est `8006`;
la variable `PORT` permet de le remplacer.

Pour les notifications push, définissez `FIREBASE_PROJECT_ID=inventa-5cad4` et fournissez des
Google Application Default Credentials autorisés à envoyer via Firebase Cloud Messaging. En local,
`GOOGLE_APPLICATION_CREDENTIALS` doit pointer vers un fichier de compte de service conservé hors
du dépôt; en production, utilisez les identifiants de service gérés. Les appareils authentifiés
associent leur jeton à leur compte; les notifications sont envoyées après la validation des
transactions et ciblées selon le rôle et le champ. Si les identifiants d’envoi ne sont pas présents,
l’opération métier reste disponible et l’échec push est journalisé. Configurez également une clé
APNs pour iOS et un certificat VAPID pour Web Push dans Firebase Console. Ne commitez aucune clé
JSON de compte de service.

HTTP Basic n’est destiné qu’au développement local protégé par réseau. Le déploiement doit être
placé derrière HTTPS. L’application utilise actuellement `spring.jpa.hibernate.ddl-auto=update`;
prévoir des migrations versionnées avant la mise en production.

## Tests

```powershell
.\mvnw.cmd test
```

Les tests utilisent une base H2 temporaire; MySQL n’est pas requis pour la suite de tests.
