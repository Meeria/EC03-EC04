# UrbanHub

Application web Spring Boot + React pour la gestion urbaine.

## Services

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:5173 | Application React (Vite) |
| Backend | http://localhost:8080 | API Spring Boot |
| Database | localhost:5432 | TimescaleDB (PostgreSQL 17) |
| SonarQube | http://localhost:9000 | Analyse de code |

## Installation

```bash
docker compose up -d
```

## Accès aux services

### Frontend
```bash
open http://localhost:5173
```

### Backend API
```bash
open http://localhost:8080
```

### Base de données (TimescaleDB)
```bash
psql -h localhost -p 5432 -U urbanhub -d urbanhub
```
Mot de passe : `urbanhub`

### SonarQube
```bash
open http://localhost:9000
```
Login par défaut : `admin` / `admin`

## Commandes Docker Compose

```bash
docker compose up -d       # Démarrer tous les services
docker compose down        # Arrêter tous les services
docker compose down -v     # Arrêter et supprimer les volumes
docker compose logs -f     # Voir les logs en temps réel
docker compose restart     # Redémarrer tous les services
```

## Analyse de code avec SonarQube

Analyser le backend :
```bash
./gradlew sonar
```

## Développement

### Prérequis
- Docker & Docker Compose
- JDK 21
- Node.js 22

### Structure
```
front/          # Application React
src/            # Code source Spring Boot
docker-compose.yml
Dockerfile
```