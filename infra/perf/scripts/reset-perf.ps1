[CmdletBinding()]
param(
    [switch]$KeepRouteCache,
    [string]$ComposeFile = "infra/perf/docker-compose.perf.yml",
    [string]$MockBaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"
$cachePattern = "route-verification:*"

if (-not $KeepRouteCache) {
    $keys = docker compose -f $ComposeFile exec -T redis redis-cli --scan --pattern $cachePattern
    foreach ($key in $keys) {
        if ($key -notlike "route-verification:*") {
            throw "Unexpected Redis key returned: $key"
        }
        docker compose -f $ComposeFile exec -T redis redis-cli UNLINK $key | Out-Null
    }
    Write-Host "Cleared route verification cache keys: $($keys.Count)"
}

$stats = Invoke-RestMethod -Method Post -Uri "$MockBaseUrl/debug/reset"
Write-Host "Reset mock counters. max_inflight=$($stats.max_inflight)"
