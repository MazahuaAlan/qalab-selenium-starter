# Versión para Windows (PowerShell). Requiere curl.exe (incluido en Windows 10/11).
#   $env:QALAB_REPORT_TOKEN = "qlp_..."; $env:QALAB_USER = "descuadre"; .\scripts\report.ps1
$ErrorActionPreference = "Continue"
$url = if ($env:QALAB_REPORT_URL) { $env:QALAB_REPORT_URL } else { "https://qa.morewater.dev/api/pipelines/ingest" }
if (-not $env:QALAB_REPORT_TOKEN) { Write-Host "report.ps1: QALAB_REPORT_TOKEN no está definido; no se envían resultados."; exit 0 }
$xmls = Get-ChildItem -Path "target/surefire-reports" -Filter "TEST-*.xml" -ErrorAction SilentlyContinue
if (-not $xmls) { Write-Host "report.ps1: no hay reportes en target/surefire-reports/"; exit 0 }
$target = if ($env:QALAB_USER) { $env:QALAB_USER } else { "estandar" }
$branch = (git rev-parse --abbrev-ref HEAD 2>$null); $commit = (git rev-parse HEAD 2>$null)
$args = @("-sS", "-X", "POST", $url, "-H", "Authorization: Bearer $($env:QALAB_REPORT_TOKEN)",
  "-F", "engine=local", "-F", "branch=$branch", "-F", "commit=$commit", "-F", "actor=$($env:USERNAME)", "-F", "trigger=manual", "-F", "target=$target",
  "-F", 'env={"browser":"chrome","language":"java"}')
foreach ($x in $xmls) { $args += @("-F", "junit=@$($x.FullName);type=application/xml") }
$resp = & curl.exe -sS -w "`n%{http_code}" @args
$lines = @($resp); $code = $lines[-1]; $body = ($lines[0..($lines.Count - 2)] -join "`n")
if ($code -eq "201") { Write-Host "report.ps1: resultados enviados: $body" } else { Write-Host "report.ps1: el panel respondió ${code}: $body" }

# Evidencia en PDF: un PDF por prueba, lotes de hasta 10 por petición (campo multipart `file` repetido).
if ($code -eq "201" -and $body -match '"run_id"\s*:\s*"?([^",}\s]+)') {
  $runId = $Matches[1]
  $pdfs = @(Get-ChildItem "target/evidence" -Filter "*.pdf" -Recurse -ErrorAction SilentlyContinue)
  if ($pdfs.Count -gt 0) {
    $base = $url -replace '/api/pipelines/ingest$', ''
    $evUrl = "$base/api/pipelines/runs/$runId/evidence"
    $ok = 0; $bad = 0
    function Send-Files($files) {
      $a = @("-sS", "-o", "NUL", "-w", "%{http_code}", "-X", "POST", $evUrl, "-H", "Authorization: Bearer $($env:QALAB_REPORT_TOKEN)")
      foreach ($f in $files) { $a += @("-F", "file=@$($f.FullName);type=application/pdf") }
      try { return (& curl.exe @a 2>$null) } catch { return "000" }
    }
    for ($i = 0; $i -lt $pdfs.Count; $i += 10) {
      $batch = @($pdfs[$i..([Math]::Min($i + 9, $pdfs.Count - 1))])
      if ((Send-Files $batch) -like "2*") { $ok += $batch.Count; continue }
      foreach ($f in $batch) { if ((Send-Files @($f)) -like "2*") { $ok++ } else { $bad++ } }   # reintento archivo por archivo
    }
    Write-Host "report.ps1: $ok PDF subidos, $bad fallidos"
  }
}
exit 0
