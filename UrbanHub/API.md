# UrbanHub — API Reference

Base URL : `http://localhost:8080`

---

## Mesures

### GET /api/measures
Retourne toutes les mesures. Filtrable par capteur via `sensor_id`.

**Paramètres (optionnels)**
| Nom | Type | Description |
|---|---|---|
| `sensor_id` | string | Identifiant métier du capteur |

**Exemples**
```
GET /api/measures
GET /api/measures?sensor_id=CAPTEUR_01
```

**Réponse**
```json
[
  {
    "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "measureId": null,
    "timestamp": "2026-04-14T10:30:00Z",
    "value": 42.5,
    "unit": "µg/m³",
    "sensorId": "CAPTEUR_01",
    "latitude": 49.1829,
    "longitude": -0.3707,
    "sensorStatus": true,
    "zoneId": "CENTRE",
    "sensorTypeId": "AIR"
  }
]
```

---

### GET /api/measures/count
Retourne le nombre total de mesures en base.

```
GET /api/measures/count
```

**Réponse**
```json
1042
```

---

### GET /api/measures/by-day
Retourne toutes les mesures d'une journée donnée (00:00:00 → 23:59:59 UTC).

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `date` | string | Date au format `yyyy-MM-dd` |

```
GET /api/measures/by-day?date=2026-04-14
```

**Réponse** : même structure que `/api/measures`

---

### GET /api/measures/by-date-range
Retourne les mesures comprises entre deux dates (inclusif).

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `from` | string | Date de début `yyyy-MM-dd` |
| `to` | string | Date de fin `yyyy-MM-dd` |

```
GET /api/measures/by-date-range?from=2026-04-01&to=2026-04-14
```

**Réponse** : même structure que `/api/measures`

---

### POST /ingest/measures
Ingère une nouvelle mesure depuis un capteur.

**Body**
```json
{
  "sensor_id": "CAPTEUR_01",
  "type": "AIR",
  "timestamp": "2026-04-14T10:30:00Z",
  "location": "49.1829;-0.3707",
  "value": 42.5,
  "unit": "µg/m³"
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `sensor_id` | oui | Identifiant métier du capteur |
| `type` | oui | Type de capteur (`AIR`, `NOISE`, `TRAFFIC`, `WEATHER`) |
| `timestamp` | oui | ISO 8601 |
| `location` | non | Coordonnées `"lat;lon"` |
| `value` | oui | Valeur mesurée |
| `unit` | oui | Unité de mesure |

**Réponse** : `200 OK` (pas de body)

---

## Capteurs

### GET /api/sensors
Retourne tous les capteurs avec leur statut calculé (`status=true` si actif sur la dernière heure).

```
GET /api/sensors
```

**Réponse**
```json
[
  {
    "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "sensorId": "CAPTEUR_01",
    "sensorTypeId": "AIR",
    "status": true
  }
]
```

---

### GET /api/sensors/status
Retourne les capteurs filtrés par statut.

**Paramètres (optionnels)**
| Nom | Type | Description |
|---|---|---|
| `alive` | boolean | `true` (défaut): capteurs actifs, `false`: capteurs inactifs |

**Exemples**
```
GET /api/sensors/status
GET /api/sensors/status?alive=false
```

**Réponse** : même structure que `/api/sensors`

---

### GET /api/sensors/status/count
Retourne le nombre de capteurs filtrés par statut.

**Paramètres (optionnels)**
| Nom | Type | Description |
|---|---|---|
| `alive` | boolean | `true` (défaut): capteurs actifs, `false`: capteurs inactifs |

```
GET /api/sensors/status/count?alive=true
```

**Réponse**
```json
12
```

---

### GET /api/sensors/status/ratio
Retourne le ratio des capteurs filtrés par statut parmi tous les capteurs (valeur entre `0.0` et `1.0`).

**Paramètres (optionnels)**
| Nom | Type | Description |
|---|---|---|
| `alive` | boolean | `true` (défaut): capteurs actifs, `false`: capteurs inactifs |

```
GET /api/sensors/status/ratio?alive=true
```

**Réponse**
```json
0.75
```

---

## Zones

### GET /api/zones
Retourne toutes les zones.

```
GET /api/zones
```

**Réponse**
```json
[
  {
    "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "zoneId": "CENTRE",
    "sensors": [
      {
        "uuid": "13b720f3-5008-4ebf-a9f1-4e66567021a9",
        "sensorId": "sensor-centre-1",
        "sensorTypeId": "AIR",
        "status": true
      }
    ]
  }
]
```

---

### GET /api/zones/count
Retourne le nombre total de zones.

```
GET /api/zones/count
```

**Réponse**
```json
5
```

---

### GET /api/zones/by-id
Retourne une zone par son identifiant métier. Retourne `404` si introuvable.

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `zone_id` | string | Identifiant métier de la zone |

```
GET /api/zones/by-id?zone_id=CENTRE
```

**Réponse**
```json
{
  "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "zoneId": "CENTRE",
  "sensors": [
    {
      "uuid": "13b720f3-5008-4ebf-a9f1-4e66567021a9",
      "sensorId": "sensor-centre-1",
      "sensorTypeId": "AIR",
      "status": true
    }
  ]
}
```

---

### POST /api/zones
Crée une nouvelle zone.

**Body**
```json
{
  "zoneId": "EAST",
  "sensorIds": ["sensor-east-1", "sensor-east-2"]
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `zoneId` | oui | Identifiant métier unique de la zone |
| `sensorIds` | non | Liste des identifiants métier des capteurs à associer |

**Réponse**
```json
{
  "uuid": "c5b2f60e-26a9-4045-b8e2-43069f50f7a2",
  "zoneId": "EAST",
  "sensors": [
    {
      "uuid": "70a3d5fe-b9af-4eb7-a59b-0f2f845eb6fd",
      "sensorId": "sensor-east-1",
      "sensorTypeId": "AIR",
      "status": true
    }
  ]
}
```

---

### PUT /api/zones/{zoneId}
Met à jour une zone existante (nom et/ou liste de capteurs).

**Paramètres de chemin**
| Nom | Type | Description |
|---|---|---|
| `zoneId` | string | Identifiant métier actuel de la zone |

**Body**
```json
{
  "zoneId": "EAST-NEW",
  "sensorIds": ["sensor-east-1"]
}
```

| Champ | Obligatoire | Description |
|---|---|---|
| `zoneId` | non | Nouvel identifiant métier |
| `sensorIds` | non | Nouvelle liste complète de capteurs associés |

**Réponse** : objet `ZoneDto` (même structure que `GET /api/zones/by-id`)

---

### DELETE /api/zones/{zoneId}
Supprime une zone existante.

**Paramètres de chemin**
| Nom | Type | Description |
|---|---|---|
| `zoneId` | string | Identifiant métier de la zone |

```
DELETE /api/zones/EAST
```

**Réponse** : `200 OK` (pas de body)

---

## Types de capteur

### GET /api/sensor-types
Retourne tous les types de capteur.

```
GET /api/sensor-types
```

**Réponse**
```json
[
  {
    "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "sensorTypeId": "AIR"
  }
]
```

---

### GET /api/sensor-types/count
Retourne le nombre total de types de capteur.

```
GET /api/sensor-types/count
```

**Réponse**
```json
4
```

---

### GET /api/sensor-types/by-id
Retourne un type de capteur par son identifiant métier. Retourne `404` si introuvable.

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `sensor_type_id` | string | Identifiant métier du type |

```
GET /api/sensor-types/by-id?sensor_type_id=AIR
```

**Réponse**
```json
{
  "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sensorTypeId": "AIR"
}
```

---

## Tendances

### GET /api/trends/sensor/latest-vs-previous
Retourne la tendance du dernier point (N) par rapport au point précédent (N-1) pour un capteur donné.

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `sensor_id` | string | Identifiant métier du capteur |

```
GET /api/trends/sensor/latest-vs-previous?sensor_id=sensor-centre-1
```

**Réponse** : objet TrendDto
```json
{
  "sensorId": "sensor-centre-1",
  "zoneId": "CENTRE",
  "timestamp": "2026-04-15T11:00:00Z",
  "value": 10.0,
  "previousValue": 8.0,
  "changeAbsolute": 2.0,
  "changePercent": 25.0,
  "comparedTo": "N-1"
}
```

> Remarque : si le capteur n'a pas au moins deux mesures, la route renvoie `null` (implémentation actuelle).

---

### GET /api/trends/sensor/latest-vs-24h
Compare le dernier point (N) au point le plus proche situé à ~24 heures avant (N-24h).

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `sensor_id` | string | Identifiant métier du capteur |

```
GET /api/trends/sensor/latest-vs-24h?sensor_id=sensor-centre-1
```

**Réponse** : objet TrendDto
```json
{
  "sensorId": "sensor-centre-1",
  "zoneId": "CENTRE",
  "timestamp": "2026-04-15T11:00:00Z",
  "value": 10.0,
  "previousValue": 6.0,
  "changeAbsolute": 4.0,
  "changePercent": 66.666664,
  "comparedTo": "N-24h"
}
```

> Logique : le service recherche la mesure la plus proche de `timestamp - 24h` (avant ou après) et l'utilise comme référence.

---

### GET /api/trends/zone/period
Calcule, pour chaque capteur d'une zone sur une période donnée, la tendance entre la dernière et la précédente mesure présentes dans cette fenêtre temporelle (pas de moyenne, seulement différences).

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `zone_id` | string | Identifiant métier de la zone |
| `start` | string (ISO) | Début de la période (ex: `2026-04-14T10:00:00Z`) |
| `end` | string (ISO) | Fin de la période (ex: `2026-04-15T12:00:00Z`) |

```
GET /api/trends/zone/period?zone_id=CENTRE&start=2026-04-14T10:00:00Z&end=2026-04-15T12:00:00Z
```

**Réponse** : liste d'objets TrendDto
```json
[
  {
    "sensorId": "sensor-centre-1",
    "zoneId": "CENTRE",
    "timestamp": "2026-04-15T11:00:00Z",
    "value": 10.0,
    "previousValue": 8.0,
    "changeAbsolute": 2.0,
    "changePercent": 25.0,
    "comparedTo": "period-last-vs-previous"
  },
  {
    "sensorId": "sensor-centre-2",
    "zoneId": "CENTRE",
    "timestamp": "2026-04-15T11:00:00Z",
    "value": 70.0,
    "previousValue": 65.0,
    "changeAbsolute": 5.0,
    "changePercent": 7.6923075,
    "comparedTo": "period-last-vs-previous"
  }
]
```

> Remarque : un capteur doit avoir au moins deux mesures dans la période pour apparaître dans la réponse.

---

### GET /api/trends/sensor/period
Calcule la tendance pour un capteur donné à l'intérieur d'une fenêtre temporelle : la dernière mesure présente dans la fenêtre (N) comparée à la précédente (N-1) elle aussi présente dans la fenêtre.

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `sensor_id` | string | Identifiant métier du capteur |
| `start` | string (ISO) | Début de la période (ex: `2026-04-10T09:00:00Z`) |
| `end` | string (ISO) | Fin de la période (ex: `2026-04-10T12:00:00Z`) |

```
GET /api/trends/sensor/period?sensor_id=sensor-centre-1&start=2026-04-14T10:00:00Z&end=2026-04-15T12:00:00Z
```

**Réponse** : objet TrendDto (ou `null` si pas au moins 2 mesures dans la fenêtre)
```json
{
  "sensorId": "sensor-centre-1",
  "zoneId": "CENTRE",
  "timestamp": "2026-04-15T11:00:00Z",
  "value": 10.0,
  "previousValue": 8.0,
  "changeAbsolute": 2.0,
  "changePercent": 25.0,
  "comparedTo": "period-N-1"
}
```

> Remarque : la période est inclusif ; la méthode ne calcule aucune moyenne, seulement la différence entre les deux dernières mesures présentes dans la fenêtre.

---

### GET /api/trends/period
Calcule la tendance pour tous les capteurs (chaque capteur) sur une période donnée : pour chaque capteur, la dernière mesure dans la fenêtre est comparée à la précédente (toutes deux contenues dans la fenêtre).

**Paramètres**
| Nom | Type | Description |
|---|---|---|
| `start` | string (ISO) | Début de la période (ex: `2026-04-11T08:00:00Z`) |
| `end` | string (ISO) | Fin de la période (ex: `2026-04-11T12:00:00Z`) |

```
GET /api/trends/period?start=2026-04-11T08:00:00Z&end=2026-04-11T12:00:00Z
```

**Réponse** : liste d'objets TrendDto (une entrée par capteur ayant au moins 2 mesures dans la fenêtre)
```json
[
  {
    "sensorId": "sensor-centre-1",
    "zoneId": "CENTRE",
    "timestamp": "2026-04-15T11:00:00Z",
    "value": 10.0,
    "previousValue": 8.0,
    "changeAbsolute": 2.0,
    "changePercent": 25.0,
    "comparedTo": "period-last-vs-previous"
  },
  {
    "sensorId": "sensor-north-1",
    "zoneId": "NORTH",
    "timestamp": "2026-04-11T11:00:00Z",
    "value": 4.0,
    "previousValue": 3.0,
    "changeAbsolute": 1.0,
    "changePercent": 33.333336,
    "comparedTo": "period-last-vs-previous"
  }
]
```

> Remarque : Les capteurs ayant moins de 2 mesures dans la période ne figurent pas dans la réponse.

---

## KPI (Moyennes)

### GET /analytic/kpi/average/bytype
Retourne une liste de KPI (moyennes) pour un type de mesure donné.

**Body (JSON)**
```json
{
  "measureType": "AIR",
  "bucket": "1 hour",
  "start": "2026-04-14T00:00:00",
  "end": "2026-04-14T23:59:59"
}
```

| Champ | Type | Description |
|---|---|---|
| `measureType` | string | Type de mesure (ex: `AIR`, `NOISE`) |
| `bucket` | string | Intervalle d'agrégation (ex: `1 hour`, `1 day`) |
| `start` | string | Date de début (sans timezone) |
| `end` | string | Date de fin (sans timezone) |

**Réponse** : liste d'objets KPI
```json
[
  {
    "bucket": "2026-04-14T10:00:00Z",
    "average": 42.5,
    "unite": "µg/m³"
  }
]
```

---

### GET /analytic/kpi/average/byzone
Retourne une carte des KPI (moyennes) regroupés par type de mesure pour une zone donnée.

**Body (JSON)**
```json
{
  "zoneId": "CENTRE",
  "bucket": "1 hour",
  "start": "2026-04-14T00:00:00",
  "end": "2026-04-14T23:59:59"
}
```

| Champ | Type | Description |
|---|---|---|
| `zoneId` | string | Identifiant métier de la zone |
| `bucket` | string | Intervalle d'agrégation (ex: `1 hour`, `1 day`) |
| `start` | string | Date de début (sans timezone) |
| `end` | string | Date de fin (sans timezone) |

**Réponse** : dictionnaire avec le type de mesure en clé et liste d'objets KPI en valeur
```json
{
  "AIR": [
    {
      "bucket": "2026-04-14T10:00:00Z",
      "average": 42.5,
      "unite": "µg/m³"
    }
  ]
}
```

---

### GET /analytic/kpi/average/bysensor
Retourne une liste de KPI (moyennes) pour un capteur spécifique.

**Body (JSON)**
```json
{
  "sensorId": "CAPTEUR_01",
  "bucket": "1 hour",
  "start": "2026-04-14T00:00:00",
  "end": "2026-04-14T23:59:59"
}
```

| Champ | Type | Description |
|---|---|---|
| `sensorId` | string | Identifiant métier du capteur |
| `bucket` | string | Intervalle d'agrégation (ex: `1 hour`, `1 day`) |
| `start` | string | Date de début (sans timezone) |
| `end` | string | Date de fin (sans timezone) |

**Réponse** : liste d'objets KPI
```json
[
  {
    "bucket": "2026-04-14T10:00:00Z",
    "average": 42.5,
    "unite": "µg/m³"
  }
]
```

---

## Codes d'erreur

| Code | Cas |
|---|---|
| `400 Bad Request` | Format de date invalide |
| `404 Not Found` | Ressource introuvable (`zone_id`, `sensor_type_id`) |
| `409 Conflict` | Conflit métier (ex: création/renommage d'une zone déjà existante) |
| `422 Unprocessable Entity` | Mesure invalide lors de l'ingestion |
