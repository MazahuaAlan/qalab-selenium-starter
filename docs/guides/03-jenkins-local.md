# Jenkins en tu máquina (gratis)

Jenkins es software libre: no pagas nada y **corre en tu equipo**, no en ningún servidor del curso. Necesitas Docker.

## 1. Levantar Jenkins
```bash
docker compose -f docker-compose.jenkins.yml up -d --build
docker compose -f docker-compose.jenkins.yml logs jenkins | grep -B1 -A3 "initial"   # contraseña inicial
```
Abre http://localhost:8080, pega la contraseña y elige **Install suggested plugins** (el Dockerfile ya trae Git, Pipeline y JUnit). La imagen incluye JDK 21, Maven y Chromium.

## 2. Guardar el token
**Manage Jenkins → Credentials → System → Global → Add Credentials**
- Kind: **Secret text** · Secret: tu token · **ID: `qalab-report-token`** (exacto).

## 3. Crear el pipeline
**New Item → Pipeline** → *Pipeline script from SCM* → Git → URL de tu repo → rama `*/java` → Script Path `Jenkinsfile`.
(Para repos públicos no necesitas credenciales de Git.)

## 4. Ejecutar
**Build with Parameters** → `PERSONA`: una en concreto o `todas`. La etapa de los usuarios con defectos queda **amarilla (UNSTABLE)** a propósito; la de `estandar` debe quedar verde.

## Conceptos que practicas
`Jenkinsfile` declarativo, parámetros, credenciales, `catchError`, `junit`, `archiveArtifacts`, agentes y plugins.

## Problemas típicos
- *Falta el plugin Pipeline:* usa la imagen de este repo (no `jenkins/jenkins` a secas).
- *Mac con Apple Silicon:* la imagen funciona en ARM (Chromium nativo); no uses Chrome de escritorio.
- *Quieres conservar tu Jenkins:* el volumen `jenkins_home` guarda todo aunque borres el contenedor.
