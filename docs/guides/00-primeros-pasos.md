# Primeros pasos

Este repo corre pruebas de Selenium (Java) contra **qalab** (https://qalab.morewater.dev) y envía los resultados al panel de **QA Playground** (https://qa.morewater.dev/pipelines).

```
tu repo ──► motor de CI (GitHub Actions / Azure Pipelines / Jenkins / tu máquina) ──► Selenium contra qalab
                                      └──► scripts/report.sh ──► panel (ejecuciones, pruebas, cobertura de defectos)
```

## 1. Crea el proyecto en el panel
1. Entra a https://qa.morewater.dev e inicia sesión.
2. Ve a **Pipelines → Nuevo proyecto**. Elige el motor y el lenguaje.
3. Copia el **token** (`qlp_...`). Solo se muestra una vez; si lo pierdes, genera otro (invalida el anterior).

## 2. Guarda el token como secreto en tu motor
El nombre es siempre `QALAB_REPORT_TOKEN`. **Nunca lo escribas dentro del código ni en el YAML.** Cada guía te dice dónde se guarda:
[GitHub Actions](01-github-actions.md) · [Azure Pipelines](02-azure-pipelines.md) · [Jenkins en tu máquina](03-jenkins-local.md) · [Sin CI](04-sin-ci.md)

## 3. Ejecuta y mira el panel
Cada ejecución crea una corrida por usuario de prueba de qalab (`estandar`, `lento`, `intermitente`…). Con `estandar` todas las pruebas deben pasar; con los demás fallan donde hay un defecto, y eso es lo que mide la **cobertura de defectos**.

## Cómo se relaciona una prueba con un defecto
El nombre de la prueba lleva el id entre corchetes: `@DisplayName("[air.tax_mismatch] El total incluye tarifa, IVA y TUA")`. El panel lo lee y lo compara con el contrato público https://qalab.morewater.dev/bugs.json.
