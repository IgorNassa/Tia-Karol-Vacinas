param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$expected = Join-Path $PSScriptRoot "..\openapi\tia-karol-api-v1.json"
$response = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/v3/api-docs"
$expectedJson = Get-Content -Raw $expected | ConvertFrom-Json | ConvertTo-Json -Depth 100 -Compress
$actualJson = $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 100 -Compress
if ($expectedJson -cne $actualJson) {
    throw "Contrato OpenAPI divergiu. Revise a mudança e versione o novo contrato conscientemente."
}
Write-Host "Contrato OpenAPI 1.0.0 confere."
