# GitHub Actions (gratis)

## Cuánto es gratis (verificado en octubre de 2026; confirma en la [página oficial](https://docs.github.com/billing/managing-billing-for-github-actions/about-billing-for-github-actions))
| Repo | Minutos |
|---|---|
| **Público** | Gratis e ilimitado con los runners estándar de GitHub |
| **Privado** (plan Free) | 2,000 minutos al mes (Linux); macOS y Windows consumen más rápido |

**Recomendación:** usa un repo público (tu copia de este repo como plantilla). Así no consumes minutos.

## Pasos
1. Pulsa **Use this template** (o haz *fork*) para crear tu repo. Mantén la rama `java`.
2. En tu repo: **Settings → Secrets and variables → Actions → New repository secret**
   - Nombre: `QALAB_REPORT_TOKEN`, valor: el token del panel.
3. Haz un *push* a la rama `java` (o ve a **Actions → Selenium contra qalab → Run workflow**).
4. Cada usuario de prueba corre en su propio trabajo (matriz). Los trabajos de los usuarios con defectos fallan a propósito y no ponen el pipeline en rojo (`continue-on-error`); el de `estandar` sí debe pasar.
5. Abre https://qa.morewater.dev/pipelines y mira la ejecución. En la pestaña **Actions** de GitHub tienes además los artefactos (reportes y capturas).

## Conceptos que practicas
`on:` (disparadores), `strategy.matrix`, `continue-on-error`, `if: always()`, secretos, artefactos, caché de Maven.

## Problemas típicos
- *No aparece nada en el panel:* ¿existe el secreto `QALAB_REPORT_TOKEN` con ese nombre exacto? Los *forks* de PRs ajenos no reciben secretos.
- *Todos los trabajos fallan por Chrome:* en `ubuntu-latest` ya viene instalado; no lo instales a mano.
