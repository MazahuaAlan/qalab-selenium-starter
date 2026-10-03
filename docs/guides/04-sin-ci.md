# Sin CI: ejecutar en tu máquina y reportar

Útil para practicar o para depurar antes de subir el pipeline.

```bash
# 1) pruebas (necesitas JDK 17+ y Maven; Selenium Manager descarga el chromedriver)
mvn test -Dqalab.user=descuadre

# 2) enviar resultados al panel (macOS/Linux/Git Bash)
export QALAB_REPORT_TOKEN=qlp_...
QALAB_USER=descuadre ./scripts/report.sh
```
Windows (PowerShell): `$env:QALAB_REPORT_TOKEN="qlp_..."; $env:QALAB_USER="descuadre"; .\scripts\report.ps1`

Opciones útiles (propiedad `-D` o variable de entorno): `qalab.headless=false` para ver el navegador, `qalab.url`, `qalab.timeout`.

Un navegador en contenedor, sin instalar Chrome:
```bash
docker run -d --name selenium -p 4444:4444 --shm-size 2g selenium/standalone-chromium
SELENIUM_REMOTE_URL=http://localhost:4444 mvn test
```
