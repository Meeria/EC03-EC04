# Pipeline CI/CD GitHub Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a GitHub Actions pipeline that builds, tests, quality-checks (ephemeral SonarQube), packages, and deploys the UrbanHub backend onto the existing AWS infra (ECR + EC2 via the existing Ansible/SSM setup), replacing the obsolete `.gitlab-ci.yml`.

**Architecture:** One workflow file (`.github/workflows/ci-cd.yml`) with five jobs chained via `needs:` — `build` → `test` → `quality` → `docker` → `deploy`. The last two are gated to `push` on `main`. `quality` runs against an ephemeral `sonarqube:community` service container (no external SaaS account). `deploy` reuses the existing Ansible playbook over AWS SSM — Terraform and `playbook.yml` are not modified (Ansible's `--extra-vars` already outranks the playbook's own `vars:` block, so the image tag can be overridden without touching the file).

**Tech Stack:** GitHub Actions, Gradle 8 / Java 21 (Temurin), Docker, AWS CLI, Ansible + `community.docker` collection, SonarQube Community Edition.

## Global Constraints

- **Never run `git commit` or `git push`.** After each task, stop and hand back to the user for review; they commit and push manually (project rule — do not deviate even if a later executor is a fresh agent with no memory of this conversation).
- Every "verify" step in this plan that requires a live GitHub Actions run depends on the user having pushed the change first. Ask them to push, then inspect the run with `gh run list` / `gh run view` — do not push yourself.
- Java 21 toolchain (`build.gradle` already pins `JavaLanguageVersion.of(21)`).
- Scope is the backend only (`UrbanHub/`) — front-end, `mock-sensors`, `mosquitto` stay out of the pipeline, consistent with the EC04-1 infra scope.
- Terraform stays manual — no `terraform apply`/`plan` anywhere in the pipeline.
- No Java dependency vulnerability scan in the pipeline (explicit user decision) — documented as an improvement, not implemented.
- No SonarCloud / hosted SonarQube account — quality gate runs against a throwaway service container, real analysis every run, no faked results.
- This repo has no local `act`/Docker-in-Docker harness — every task is verified by pushing and reading the real GitHub Actions run, not by local simulation.

---

## Prérequis manuels (à faire par l'utilisateur avant la Task 1)

Ces valeurs ne peuvent pas être déduites du code — à ajouter dans `Settings > Secrets and variables > Actions` du repo GitHub avant de tester quoi que ce soit :

- **Secrets** : `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (utilisateur IAM avec accès `ecr:*`, `secretsmanager:GetSecretValue` sur `urbanhub/*`, `ssm:StartSession`).
- **Variables** : `INSTANCE_ID` (sortie Terraform `backend_instance_id`), `SSM_TRANSFER_BUCKET` (sortie Terraform `ssm_transfer_bucket`).

Note de simplification par rapport au spec initial : seules ces deux variables sont réellement nécessaires. `ECR_REPOSITORY_URL`, `RDS_ENDPOINT` et `DB_PASSWORD_SECRET_ID` restent dans `playbook.yml` (déjà committé, déjà la source de vérité) — les dupliquer en variables GitHub créerait une deuxième source qui pourrait diverger, sans bénéfice.

---

### Task 1: Squelette du workflow + job `build`

**Files:**
- Create: `.github/workflows/ci-cd.yml`

**Interfaces:**
- Produces: workflow `CI/CD`, job `build` (aucune dépendance)

- [ ] **Step 1: Créer le fichier workflow**

```yaml
name: CI/CD

on:
  push:
    branches: ["**"]
  pull_request:
    branches: ["**"]

jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: UrbanHub
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - uses: gradle/actions/setup-gradle@v4
      - name: Build
        run: ./gradlew assemble --no-daemon
```

- [ ] **Step 2: Stop pour review**

Ne pas committer. Montrer le diff à l'utilisateur et attendre qu'il commit/push lui-même (branche de son choix, pas forcément `main`).

- [ ] **Step 3: Vérifier après push**

Une fois poussé par l'utilisateur :
```bash
gh run list --limit 1
gh run view --log
```
Expected: job `build` en succès (✓).

---

### Task 2: Job `test` (Jacoco)

**Files:**
- Modify: `.github/workflows/ci-cd.yml`

**Interfaces:**
- Consumes: job `build` (Task 1)
- Produces: job `test`, artifact `jacoco-report`

- [ ] **Step 1: Ajouter le job `test` juste après `build:`**

```yaml
  test:
    runs-on: ubuntu-latest
    needs: build
    defaults:
      run:
        working-directory: UrbanHub
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - uses: gradle/actions/setup-gradle@v4
      - name: Test
        run: ./gradlew test jacocoTestReport --no-daemon
      - name: Upload Jacoco report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: UrbanHub/build/reports/jacoco/test
```

- [ ] **Step 2: Stop pour review**

Ne pas committer. Attendre que l'utilisateur commit/push.

- [ ] **Step 3: Vérifier après push**

```bash
gh run list --limit 1
gh run view --log
```
Expected: jobs `build` et `test` en succès, artifact `jacoco-report` visible dans l'onglet Actions du run (`gh run view --log | grep -i artifact` ou consultation directe dans l'UI).

---

### Task 3: Job `quality` — SonarQube éphémère, analyse de base

**Files:**
- Modify: `.github/workflows/ci-cd.yml`

**Interfaces:**
- Consumes: job `test` (Task 2)
- Produces: job `quality`, step output `steps.sonar_token.outputs.token` (utilisé par Task 4)

- [ ] **Step 1: Ajouter le job `quality` juste après `test:`**

```yaml
  quality:
    runs-on: ubuntu-latest
    needs: test
    services:
      sonarqube:
        image: sonarqube:community
        ports:
          - 9000:9000
        options: >-
          --health-cmd="curl -f http://localhost:9000/api/system/health || exit 1"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=30
    defaults:
      run:
        working-directory: UrbanHub
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - uses: gradle/actions/setup-gradle@v4
      - name: Wait for SonarQube to be ready
        run: |
          for i in $(seq 1 30); do
            status=$(curl -s http://localhost:9000/api/system/health | grep -o '"health":"GREEN"' || true)
            if [ -n "$status" ]; then echo "SonarQube ready"; exit 0; fi
            echo "Waiting for SonarQube... ($i/30)"
            sleep 5
          done
          echo "SonarQube did not become ready in time" >&2
          exit 1
      - name: Generate a one-shot analysis token
        id: sonar_token
        run: |
          TOKEN=$(curl -s -u admin:admin -X POST "http://localhost:9000/api/user_tokens/generate" -d "name=ci-$(date +%s)" | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")
          if [ -z "$TOKEN" ]; then echo "Failed to generate SonarQube token" >&2; exit 1; fi
          echo "::add-mask::$TOKEN"
          echo "token=$TOKEN" >> "$GITHUB_OUTPUT"
      - name: Run SonarQube analysis
        run: ./gradlew sonarqube -Dsonar.host.url=http://localhost:9000 -Dsonar.token=${{ steps.sonar_token.outputs.token }} --no-daemon
```

- [ ] **Step 2: Stop pour review**

Ne pas committer. Attendre que l'utilisateur commit/push.

- [ ] **Step 3: Vérifier après push**

```bash
gh run view --log
```
Expected: le step "Run SonarQube analysis" termine avec `EXECUTION SUCCESS`. Si le step "Generate a one-shot analysis token" échoue avec un token vide, c'est le risque technique déjà noté dans le spec (mot de passe par défaut `admin`/`admin` refusé par une instance qui force son changement) — dans ce cas, ajouter un step avant la génération de token qui appelle `POST /api/users/change_password` avec l'ancien et le nouveau mot de passe `admin`, puis générer le token avec le nouveau mot de passe.

---

### Task 4: Quality gate réel + artifact

**Files:**
- Modify: `.github/workflows/ci-cd.yml`

**Interfaces:**
- Consumes: `steps.sonar_token.outputs.token` (Task 3)
- Produces: artifact `sonarqube-quality-gate`, job `quality` en échec si le gate n'est pas `OK`

- [ ] **Step 1: Ajouter ces deux steps à la fin du job `quality` (après "Run SonarQube analysis")**

```yaml
      - name: Check quality gate
        run: |
          TASK_FILE=$(find .scannerwork -name report-task.txt | head -n1)
          CE_TASK_URL=$(grep '^ceTaskUrl=' "$TASK_FILE" | cut -d= -f2-)
          for i in $(seq 1 20); do
            CE_STATUS=$(curl -s -u "${{ steps.sonar_token.outputs.token }}:" "$CE_TASK_URL" | python3 -c "import sys, json; print(json.load(sys.stdin)['task']['status'])")
            if [ "$CE_STATUS" = "SUCCESS" ]; then break; fi
            if [ "$CE_STATUS" = "FAILED" ] || [ "$CE_STATUS" = "CANCELED" ]; then echo "Analysis processing failed: $CE_STATUS" >&2; exit 1; fi
            echo "Waiting for analysis to be processed... ($i/20)"
            sleep 5
          done
          ANALYSIS_ID=$(curl -s -u "${{ steps.sonar_token.outputs.token }}:" "$CE_TASK_URL" | python3 -c "import sys, json; print(json.load(sys.stdin)['task']['analysisId'])")
          curl -s -u "${{ steps.sonar_token.outputs.token }}:" "http://localhost:9000/api/qualitygates/project_status?analysisId=$ANALYSIS_ID" -o quality-gate-status.json
          cat quality-gate-status.json
          GATE_STATUS=$(python3 -c "import json; print(json.load(open('quality-gate-status.json'))['projectStatus']['status'])")
          echo "Quality gate status: $GATE_STATUS"
          if [ "$GATE_STATUS" != "OK" ]; then echo "Quality gate failed" >&2; exit 1; fi
      - name: Upload quality gate report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: sonarqube-quality-gate
          path: UrbanHub/quality-gate-status.json
```

- [ ] **Step 2: Stop pour review**

Ne pas committer. Attendre que l'utilisateur commit/push.

- [ ] **Step 3: Vérifier après push**

```bash
gh run view --log
```
Expected: step "Check quality gate" affiche `Quality gate status: OK` (ou fait échouer le job si un vrai defaut de qualite existe — c'est le comportement voulu, pas un bug). Artifact `sonarqube-quality-gate` present sur le run.

---

### Task 5: Job `docker` — build + push ECR (main uniquement)

**Files:**
- Modify: `.github/workflows/ci-cd.yml`

**Interfaces:**
- Consumes: job `quality` (Task 4)
- Produces: job `docker`, image poussee sur ECR taguee `${{ github.sha }}`

- [ ] **Step 1: Ajouter le job `docker` juste apres `quality:`**

```yaml
  docker:
    runs-on: ubuntu-latest
    needs: quality
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: eu-west-3
      - name: Login to ECR
        id: ecr_login
        uses: aws-actions/amazon-ecr-login@v2
      - name: Build and push image
        working-directory: UrbanHub
        env:
          ECR_REGISTRY: ${{ steps.ecr_login.outputs.registry }}
        run: |
          IMAGE="$ECR_REGISTRY/urbanhub-backend:${{ github.sha }}"
          docker build -t "$IMAGE" .
          docker push "$IMAGE"
```

- [ ] **Step 2: Stop pour review**

Ne pas committer. Attendre que l'utilisateur commit/push **sur `main`** (c'est le seul declencheur de ce job — pas la peine de tester sur une branche).

- [ ] **Step 3: Verifier apres push sur `main`**

```bash
gh run view --log
aws ecr describe-images --repository-name urbanhub-backend --region eu-west-3
```
Expected: job `docker` en succes, l'image avec le tag = SHA du commit apparait dans `describe-images`.

---

### Task 6: Job `deploy` — Ansible via SSM (main uniquement)

**Files:**
- Modify: `.github/workflows/ci-cd.yml`

**Interfaces:**
- Consumes: job `docker` (Task 5), variables de repo `vars.INSTANCE_ID` / `vars.SSM_TRANSFER_BUCKET` (prerequis manuel)
- Produces: job `deploy`, backend redemarre sur l'EC2 avec l'image taguee `${{ github.sha }}`

- [ ] **Step 1: Ajouter le job `deploy` juste apres `docker:`**

```yaml
  deploy:
    runs-on: ubuntu-latest
    needs: docker
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: eu-west-3
      - name: Install session-manager-plugin
        run: |
          curl -s "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/ubuntu_64bit/session-manager-plugin.deb" -o session-manager-plugin.deb
          sudo dpkg -i session-manager-plugin.deb
      - name: Install Ansible and collections
        run: |
          pip install --user ansible boto3 botocore
          ansible-galaxy collection install community.docker
      - name: Generate inventory.ini
        working-directory: UrbanHub/infra/ansible
        run: |
          sed -e "s#<INSTANCE_ID>#${{ vars.INSTANCE_ID }}#" \
              -e "s#<SSM_TRANSFER_BUCKET>#${{ vars.SSM_TRANSFER_BUCKET }}#" \
              inventory.ini.tmpl > inventory.ini
      - name: Deploy via Ansible
        working-directory: UrbanHub/infra/ansible
        run: ansible-playbook playbook.yml --extra-vars "image_tag=${{ github.sha }}"
```

Note : `--extra-vars` a une precedence superieure aux `vars:` du play dans `playbook.yml` (qui garde `image_tag: latest` comme defaut pour un lancement manuel local) — aucune modification de `playbook.yml` n'est necessaire.

- [ ] **Step 2: Stop pour review**

Ne pas committer. Rappeler explicitement a l'utilisateur : ce job, une fois pousse sur `main`, va reellement redeployer le backend sur l'EC2 de production de l'epreuve. Attendre confirmation avant qu'il pousse.

- [ ] **Step 3: Verifier apres push sur `main`**

```bash
gh run view --log
curl http://<backend_public_ip>:8080/actuator/health   # ou l'endpoint dispo, selon API.md
```
Expected: job `deploy` en succes, l'API repond, et les logs applicatifs (via `docker compose logs` en session SSM si besoin) montrent le redemarrage avec la nouvelle image.

---

### Task 7: Retirer l'ancien `.gitlab-ci.yml`

**Files:**
- Delete: `.gitlab-ci.yml`

- [ ] **Step 1: Supprimer le fichier**

```bash
git rm .gitlab-ci.yml
```

- [ ] **Step 2: Stop pour review**

Ne pas committer. Expliquer pourquoi a l'utilisateur : GitHub n'execute pas `.gitlab-ci.yml`, il ne sert plus qu'a confusion (deploiement mort vers l'ancien serveur on-premise de l'equipe). Attendre qu'il commit.

---

### Task 8: Documentation du pipeline (EC03-1 / EC03-2)

**Files:**
- Create: `docs/EC03-1-cicd.md`

- [ ] **Step 1: Ecrire la documentation**

```markdown
# EC03 Partie 1 - Pipeline CI/CD

## Plateforme

Le depot a bascule de GitLab vers GitHub (github.com/Meeria/EC03-EC04), decision validee avec le formateur. Le pipeline est donc un workflow GitHub Actions (`.github/workflows/ci-cd.yml`), et non un `.gitlab-ci.yml`.

## Declenchement

| Evenement | Jobs |
|---|---|
| push / pull_request, toute branche | build, test, quality |
| push sur main | + docker (build/push ECR), deploy (Ansible vers l'EC2) |

## Etapes

- **build** : `./gradlew assemble` - verifie que le projet compile de maniere reproductible.
- **test** : `./gradlew test jacocoTestReport` sur H2 en memoire, rapport Jacoco publie en artifact.
- **quality** : analyse SonarQube reelle contre une instance ephemere (`sonarqube:community` en `services:` du job, jamais persistee), quality gate qui fait echouer le job si le resultat n'est pas OK. Choix fait pour ne pas dependre d'un compte SonarCloud externe (plus accessible).
- **docker** (main uniquement) : build de l'image depuis le `Dockerfile` existant, tag = SHA du commit (tracabilite), push vers l'ECR existant (`ecr.tf`, scan de vulnerabilites deja actif via `scan_on_push`).
- **deploy** (main uniquement) : Ansible via SSM (pas de SSH), reutilise le playbook existant de l'EC04-1, image deployee = celle qui vient d'etre poussee (meme SHA).

## Prerequis (secrets/variables GitHub)

- Secrets : `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`.
- Variables : `INSTANCE_ID`, `SSM_TRANSFER_BUCKET` (sorties Terraform, stables tant que l'infra n'est pas recreee).

## Interpretation des resultats

- Rapport de tests : artifact `jacoco-report` sur chaque run.
- Rapport qualite : artifact `sonarqube-quality-gate` (statut reel du quality gate SonarQube, pas simule).
- Tracabilite : chaque image deployee est identifiable par le SHA du commit qui l'a produite (tag ECR).

## Limites et pistes d'amelioration

- **Pas de scan de vulnerabilites sur les dependances Java** dans le pipeline (risque #8, deja documente dans `EC04-2-securite.md`) : choix assume pour rester dans le perimetre minimal de l'epreuve. Piste : `OWASP Dependency-Check` (plugin Gradle) en job supplementaire.
- **Authentification AWS par cles statiques** (`AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`) plutot que par OIDC (role assume, sans secret long-lived). Plus simple a mettre en place pour l'epreuve ; OIDC serait la version "production".
- **Terraform hors pipeline** : l'infra reste provisionnee/modifiee manuellement ; seul le cycle applicatif (build -> test -> quality -> image -> deploy) est automatise.
```

- [ ] **Step 2: Stop pour review**

Ne pas committer. Attendre que l'utilisateur relise et commit.

---

## Self-Review (fait par l'agent qui ecrit ce plan, pas a refaire par l'executant)

- **Couverture du spec** : build ✓ (Task 1), test ✓ (Task 2), quality ephemere + gate reel ✓ (Task 3-4), docker/ECR + tag SHA ✓ (Task 5), deploy Ansible/SSM avec image_tag dynamique ✓ (Task 6), retrait `.gitlab-ci.yml` ✓ (Task 7), documentation + limites ✓ (Task 8). Terraform manuel = contrainte globale, aucune tache necessaire.
- **Placeholders** : aucun "TBD"/"a completer" dans les steps ; le seul point marque explicitement incertain (token SonarQube sur instance fraiche) a un repli concret documente dans Task 3 Step 3, pas un placeholder vague.
- **Coherence** : le nom du job/registre ECR (`urbanhub-backend`) correspond a `ecr.tf` ; les placeholders `<INSTANCE_ID>`/`<SSM_TRANSFER_BUCKET>` du `sed` (Task 6) correspondent exactement a `inventory.ini.tmpl` ; `steps.sonar_token.outputs.token` (Task 3) est bien reutilise tel quel en Task 4 (meme nom de step `sonar_token`, meme cle `token`).
