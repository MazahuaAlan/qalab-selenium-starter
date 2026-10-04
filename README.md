# qalab-selenium-starter

Plantilla de automatización con **Selenium + JUnit 5** (Java 17+) contra [MoreWater qalab](https://qalab.morewater.dev), lista para correr en **GitHub Actions, Azure Pipelines o Jenkins** y enviar los resultados al panel de [QA Playground](https://qa.morewater.dev/pipelines).

## Ramas por lenguaje
| Rama | Stack |
|---|---|
| `java` | Maven · Selenium 4 · JUnit 5 · AssertJ (esta) |
| `python` | pytest · Selenium (próximamente) |

## Estructura (rama `java`)
```
src/test/java/dev/morewater/qalab/
  config/Config.java          URL, usuario de prueba, modo headless… (propiedad -D o variable de entorno)
  core/                       DriverFactory, BaseTest (login + captura al fallar)
  pages/                      Page Objects: LoginPage, AirPage, BankPage
  tests/                      AirTests, BankTests, LoginTests
.github/workflows/selenium.yml   GitHub Actions (matriz por usuario de prueba)
Jenkinsfile · jenkins/ · docker-compose.jenkins.yml   Jenkins local
azure-pipelines.yml              Azure Pipelines
scripts/report.sh · report.ps1   Envío de resultados al panel
docs/guides/                     Guías de uso gratuito de cada motor
```

## Ideas clave
- **Pruebas de contrato:** cada prueba verifica el comportamiento *correcto*. Con el usuario `estandar` pasan todas; con otro usuario de prueba (`lento`, `descuadre`…) fallan donde qalab tiene un defecto. El panel mide cuántos defectos detecta tu suite.
- **El id del defecto va en el nombre:** `@DisplayName("[air.tax_mismatch] …")`. La lista oficial está en https://qalab.morewater.dev/bugs.json.
- **Localizadores `data-test`:** usa el modo **Inspector** de qalab (botón «Inspector» o `?inspect=1`) para copiar el selector listo.

## Empezar
1. Lee [docs/guides/00-primeros-pasos.md](docs/guides/00-primeros-pasos.md).
2. Elige tu motor: [GitHub Actions](docs/guides/01-github-actions.md) · [Azure Pipelines](docs/guides/02-azure-pipelines.md) · [Jenkins local](docs/guides/03-jenkins-local.md) · [sin CI](docs/guides/04-sin-ci.md).
3. Evidencia en PDF de cada prueba (una captura por paso): [docs/guides/05-evidencia-pdf.md](docs/guides/05-evidencia-pdf.md).

Sitio de práctica con defectos intencionales: no uses datos reales.
