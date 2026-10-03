# Azure Pipelines (gratis)

## Cuánto es gratis (verificado en octubre de 2026; confirma en la [documentación](https://learn.microsoft.com/azure/devops/pipelines/licensing/concurrent-jobs))
- Cada organización incluye **1 trabajo paralelo gratuito con agente propio (self-hosted)** y minutos ilimitados.
- Para **agentes de Microsoft** (`vmImage: ubuntu-latest`): proyectos privados con 1 trabajo paralelo y 1,800 minutos al mes, **pero en organizaciones nuevas la concesión no es automática**: hay que pedirla en https://aka.ms/azpipelines-parallelism-request (responden en 2–3 días hábiles). Hasta entonces verás el error *«No hosted parallelism has been purchased or granted»*.
- Los proyectos **públicos** ya no se pueden crear.

## Opción A: agente propio (gratis y sin esperar) ← recomendada para empezar
1. Crea tu organización y proyecto en https://dev.azure.com.
2. **Project settings → Agent pools → Default → New agent**: descarga el agente para tu sistema y sigue los 3 comandos (`config` y `run`).
   Necesitas en esa máquina: JDK 21, Maven, Chrome y `bash` (en Windows, Git Bash).
3. En `azure-pipelines.yml` cambia
   ```yaml
   pool:
     vmImage: ubuntu-latest
   ```
   por
   ```yaml
   pool:
     name: Default
   ```
## Opción B: agentes de Microsoft
Pide la concesión en el formulario y espera la respuesta; después deja `vmImage: ubuntu-latest`.

## Crear el pipeline
1. **Pipelines → New pipeline → GitHub** (autoriza la app de Azure Pipelines en tu repo) → elige el repo → **Existing Azure Pipelines YAML file** → rama `java` → `/azure-pipelines.yml`.
2. **Variables → New variable**: nombre `QALAB_REPORT_TOKEN`, valor el token, marca **Keep this value secret**.
3. **Run.** El YAML lo pasa al script con `env:` (los secretos no se heredan solos).
4. Mira los resultados en la pestaña **Tests** de Azure y en https://qa.morewater.dev/pipelines.

## Conceptos que practicas
`pool` (dónde corre), `strategy.matrix`, `condition: always()`, variables secretas, `PublishTestResults`, artefactos.
