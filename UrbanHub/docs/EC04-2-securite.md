# EC04 Partie 2 - Securisation de l'application

## Risques

Niveau = impact x probabilite, sur une echelle simple : Critique, Eleve, Modere, Faible.

| # | Risque | Categorie | Impact | Probabilite | Niveau | Statut |
|---|---|---|---|---|---|---|
| 1 | Aucune authentification/autorisation sur l'API (pas de Spring Security, tous les endpoints `/api/**` sont publics) | Application | Eleve : n'importe qui peut lire ou modifier toutes les donnees | Elevee : API exposee publiquement en HTTP | **Critique** | A traiter |
| 2 | CORS ouvert a tous les domaines (`allowedOriginPattern("*")`, toutes methodes, tous headers) | Application | Modere : `allowCredentials=false` limite l'impact reel aujourd'hui, mais aucun controle si un jour des cookies/tokens sont introduits | Elevee | Modere | A traiter |
| 3 | Identifiants de base de donnees en dur dans le code (`application.properties`, `application-production.properties`, `docker-compose.yml`, `.env.example`) | Donnees / secrets | Eleve : fuite du mot de passe si le depot est expose | Elevee : deja present dans l'historique Git | **Critique** | Mitige pour le cloud (secret genere dans Secrets Manager, EC04-1) ; le code source garde toujours ces valeurs par defaut |
| 4 | Broker MQTT sans authentification (`allow_anonymous true`, pas de TLS) | Infrastructure / reseau | Eleve : injection de fausses mesures, ecoute du flux | Moderee : depend de l'exposition reseau du broker | Eleve | Hors perimetre EC04-1 (Mosquitto reste en local), pas encore traite |
| 5 | Documentation API (Swagger/OpenAPI) accessible sans authentification | Application | Modere : revele toute la structure de l'API | Elevee | Modere | A traiter (lie au risque 1) |
| 6 | Pas de HTTPS sur l'API exposee dans le cloud | Infrastructure / reseau | Eleve : donnees et futurs identifiants en clair sur le reseau | Moderee | Eleve | Limite assumee, documentee en EC04-1 |
| 7 | Identifiants par defaut SonarQube (`admin`/`admin`), jamais changes | Acces / identite | Modere : acces a l'outil d'analyse, pas aux donnees metier | Elevee si l'outil est expose au-dela du poste local | Modere | A traiter |
| 8 | Aucune analyse de vulnerabilites sur les dependances Java (le scan ECR ne couvre que l'image conteneur, pas les librairies applicatives) | Chaine logicielle | Variable, selon les CVE existantes | Moderee | Modere | A mettre en place |
| 9 | Permissions IAM de l'operateur (celui qui lance Terraform/Ansible) non definies precisement, identifiants personnels potentiellement larges | Acces / identite | Eleve si ces identifiants sont larges (type administrateur) | Moderee | Eleve | A traiter |
| 10 | Absence de logs et de supervision | Detection | Eleve : un incident pourrait passer inapercu | Elevee : rien n'est en place actuellement | Eleve | Objet du plan de supervision (section suivante) |
| 11 | Reference IP residuelle commentee dans `application.properties` (`149.202.77.193:7819`) | Application / hygiene | Faible : fuite d'information mineure sur une infra passee | Faible | Faible | A nettoyer |
| 12 | Pas de rotation automatique du secret RDS | Donnees / secrets | Faible : le secret reste valide indefiniment si compromis | Faible | Faible | Piste d'amelioration, Secrets Manager le permet nativement |

Priorite de traitement dans cette mission : les risques Critique et Eleve (1, 2, 3, 4, 6, 8, 9, 10) sont couverts dans "Configuration securite" et "Plan de supervision" ci-dessous. Les risques Faible (11, 12) sont notes mais pas necessairement traites dans cette iteration.

## Configuration securite

(IAM, politiques, regles reseau, gestion des secrets - a completer)

## Plan de supervision

(logs, metriques, alertes - a completer)
