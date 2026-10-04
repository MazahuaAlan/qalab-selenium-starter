# 05 · Evidencia en PDF (una captura por paso)

Cada prueba genera su **propio PDF** con lo que hizo el navegador, paso a paso. Sirve como evidencia de ejecución y para revisar un fallo sin volver a correr nada.

## Cómo funciona

- `DriverFactory.create()` envuelve el `WebDriver` con `EventFiringDecorator` (Selenium 4). Tras abrir una URL, hacer clic, escribir (`sendKeys`) o limpiar un campo se toma una captura y se le pone una etiqueta legible: `Clic en [data-test='login-button'] "Entrar"`, `Escribir "estandar" en [data-test='username']`, `Abrir https://…`. Las contraseñas se enmascaran. Si la captura es idéntica a la anterior (mismo hash), se omite.
- El driver decorado sigue siendo `JavascriptExecutor` y `TakesScreenshot`; las pruebas y los page objects no cambian.
- La extensión `ScreenshotOnFailure` (registrada en `BaseTest`) abre el bloque de la prueba, toma una captura final («Estado final») y, si falla, una captura con el mensaje de error.
- Las imágenes **nunca se escriben a disco**: viven en memoria, se reducen a 640 px de ancho, JPEG calidad 50, y se incrustan en el PDF (OpenPDF) al terminar la prueba.

## Dónde quedan

```
target/evidence/<persona>/<NombreVisible>.pdf
```

`NombreVisible` es el `@DisplayName` sin el prefijo `[bug.id]`, saneado a `[A-Za-z0-9_.-]`; conserva el id del caso (`A_AIR_017…`). Cada PDF tiene una primera página con nombre, clase, módulo, persona, fecha UTC, resultado (APROBADA/FALLIDA), duración y error; después los pasos, dos por página, sin cortar ninguna captura.

`scripts/report.sh` / `report.ps1` suben los PDF al panel tras el `ingest` (lotes de hasta 10 por petición a `/api/pipelines/runs/<run_id>/evidence`; si un lote falla, reintenta archivo por archivo; nunca rompe el pipeline). En GitHub Actions la máquina virtual se destruye al terminar el trabajo, así que no queda nada más que lo subido al panel.

## Desactivarla

```
mvn test -Dqalab.evidence=false        # o export QALAB_EVIDENCE=false
```

Sin evidencia no se decora el driver ni se genera ningún PDF.

## Pruebas que miden tiempo

Cada captura cuesta unos 70–150 ms (la primera, ~1 s). Si una prueba mide un intervalo muy ajustado, suspende la captura durante la medición:

```java
Evidence.pause();
try { /* acciones medidas */ } finally { Evidence.resume(); }
```

## Tamaños medidos

Cada captura pesa ~20–25 KB. Una prueba típica (8–10 pasos) da un PDF de 150–230 KB; las de flujos largos (pago, reserva) llegan a ~500 KB.
