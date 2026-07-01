# Verify Conni-X-Pro API connectivity with CONNI_API_KEY.
param()

$BaseUrl = if ($env:CONNI_API_BASE_URL) { $env:CONNI_API_BASE_URL } else { "http://localhost:9898/api" }
$ApiKey = $env:CONNI_API_KEY

if (-not $ApiKey) {
    Write-Error "CONNI_API_KEY is not set"
    exit 1
}

$headers = @{ "X-Api-Key" = $ApiKey }

Write-Host "Base URL: $BaseUrl"
Write-Host "Checking health..."
$health = Invoke-RestMethod -Uri "$BaseUrl/client/health" -Headers $headers
Write-Host "  health: $($health | ConvertTo-Json -Compress)"

Write-Host "Checking credits..."
$credits = Invoke-RestMethod -Uri "$BaseUrl/user/get/credits" -Headers $headers
Write-Host "  credits: $($credits.data)"

Write-Host "OK: connection verified"
