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
Get-ChildItem "target/screenshots/*.png" -ErrorAction SilentlyContinue | Select-Object -First 20 | ForEach-Object { $args += @("-F", "files=@$($_.FullName);type=image/png") }
& curl.exe @args
exit 0
