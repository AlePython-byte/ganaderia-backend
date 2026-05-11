# Uso:
#   powershell -ExecutionPolicy Bypass -File .\scripts\load-test-device-ingestion.ps1 `
#     -BaseUrl "http://localhost:8080" `
#     -DeviceToken "COLLAR-001" `
#     -DeviceSecret "secret" `
#     -Requests 10 `
#     -Concurrency 1 `
#     -DelayMs 100
#
# Escenarios sugeridos:
#   Smoke:  -Requests 10  -Concurrency 1  -DelayMs 100
#   Light:  -Requests 100 -Concurrency 5  -DelayMs 50
#   Medium: -Requests 500 -Concurrency 20 -DelayMs 20
#
# No ejecutar high stress por defecto. El script no imprime DeviceSecret ni firmas HMAC completas.

param(
    [Alias("BASE_URL")]
    [string]$BaseUrl = "http://localhost:8080",

    [Alias("DEVICE_TOKEN")]
    [Parameter(Mandatory = $true)]
    [string]$DeviceToken,

    [Alias("DEVICE_SECRET")]
    [Parameter(Mandatory = $true)]
    [string]$DeviceSecret,

    [ValidateRange(1, 100000)]
    [int]$Requests = 20,

    [ValidateRange(1, 200)]
    [int]$Concurrency = 1,

    [ValidateRange(0, 60000)]
    [int]$DelayMs = 100,

    [double]$Latitude = 1.2136,

    [double]$Longitude = -77.2811,

    [ValidateRange(0, 100)]
    [int]$BatteryLevel = 85,

    [ValidateRange(0, 10000)]
    [double]$GpsAccuracy = 8.5,

    [switch]$VerboseErrors
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Mask-Value {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    if ($Value.Length -le 8) {
        return ("*" * $Value.Length)
    }

    return "{0}...{1}" -f $Value.Substring(0, 4), $Value.Substring($Value.Length - 4)
}

function Get-ErrorBody {
    param([System.Exception]$Exception)

    if ($null -eq $Exception -or $null -eq $Exception.Response) {
        return $null
    }

    try {
        $stream = $Exception.Response.GetResponseStream()
        if ($null -eq $stream) {
            return $null
        }

        $reader = [System.IO.StreamReader]::new($stream)
        try {
            $body = $reader.ReadToEnd()
            if ([string]::IsNullOrWhiteSpace($body)) {
                return $null
            }
            return $body
        }
        finally {
            $reader.Dispose()
        }
    }
    catch {
        return $null
    }
}

function Truncate-Text {
    param(
        [string]$Value,
        [int]$MaxLength = 500
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return "-"
    }

    if ($Value.Length -le $MaxLength) {
        return $Value
    }

    return $Value.Substring(0, $MaxLength) + "...[truncated]"
}

$path = "/api/device/locations"
$trimmedBaseUrl = $BaseUrl.TrimEnd("/")
$endpoint = "$trimmedBaseUrl$path"
$effectiveConcurrency = [Math]::Min($Concurrency, $Requests)
$jobScript = {
    param(
        [int]$RequestNumber,
        [string]$Endpoint,
        [string]$Path,
        [string]$Token,
        [string]$Secret,
        [double]$Lat,
        [double]$Lon,
        [int]$Battery,
        [double]$Accuracy,
        [bool]$ShowVerboseErrors
    )

    $requestInstantUtc = [DateTimeOffset]::UtcNow
    $timestampHeader = $requestInstantUtc.ToString("yyyy-MM-ddTHH:mm:ss'Z'")
    $bodyTimestamp = $requestInstantUtc.ToString("yyyy-MM-ddTHH:mm:ss")
    $nonce = ([Guid]::NewGuid().ToString() + "-" + $RequestNumber)

    $bodyObject = [ordered]@{
        latitude = $Lat
        longitude = $Lon
        timestamp = $bodyTimestamp
        batteryLevel = $Battery
        gpsAccuracy = $Accuracy
    }
    $bodyJson = $bodyObject | ConvertTo-Json -Compress

    $canonicalRequest = "POST`n$Path`n$timestampHeader`n$nonce`n$bodyJson"
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([System.Text.Encoding]::UTF8.GetBytes($Secret))
    try {
        $signatureBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($canonicalRequest))
    }
    finally {
        $hmac.Dispose()
    }

    $headers = @{
        "X-Device-Token" = $Token
        "X-Device-Timestamp" = $timestampHeader
        "X-Device-Nonce" = $nonce
        "X-Device-Signature" = [Convert]::ToBase64String($signatureBytes)
    }

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        Invoke-RestMethod `
            -Method Post `
            -Uri $Endpoint `
            -Headers $headers `
            -ContentType "application/json" `
            -Body $bodyJson `
            -TimeoutSec 30 | Out-Null

        $stopwatch.Stop()
        return [pscustomobject]@{
            RequestNumber = $RequestNumber
            Success = $true
            StatusCode = 200
            LatencyMs = $stopwatch.ElapsedMilliseconds
            ErrorSummary = $null
        }
    }
    catch {
        $stopwatch.Stop()
        $statusCode = "NO_STATUS"
        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }
            catch {
                $statusCode = "NO_STATUS"
            }
        }

        $errorText = $_.Exception.Message
        if ($ShowVerboseErrors -and $_.Exception.Response) {
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                if ($null -ne $stream) {
                    $reader = [System.IO.StreamReader]::new($stream)
                    try {
                        $body = $reader.ReadToEnd()
                        if (-not [string]::IsNullOrWhiteSpace($body)) {
                            $errorText = $body
                        }
                    }
                    finally {
                        $reader.Dispose()
                    }
                }
            }
            catch {
                $errorText = $_.Exception.Message
            }
        }

        return [pscustomobject]@{
            RequestNumber = $RequestNumber
            Success = $false
            StatusCode = $statusCode
            LatencyMs = $stopwatch.ElapsedMilliseconds
            ErrorSummary = $errorText
        }
    }
}

Write-Host "Device ingestion load test"
Write-Host "BaseUrl: $trimmedBaseUrl"
Write-Host "Endpoint: $path"
Write-Host "DeviceToken: $(Mask-Value -Value $DeviceToken)"
Write-Host "Requests: $Requests"
Write-Host "Concurrency: $effectiveConcurrency"
Write-Host "DelayMs: $DelayMs"
Write-Host "Latitude: $Latitude"
Write-Host "Longitude: $Longitude"
Write-Host "BatteryLevel: $BatteryLevel"
Write-Host "GpsAccuracy: $GpsAccuracy"
Write-Host ""

$allResults = New-Object System.Collections.Generic.List[object]
$runningJobs = @()
$startedAt = [DateTimeOffset]::UtcNow
$stopwatchTotal = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 1; $i -le $Requests; $i++) {
    while (@($runningJobs | Where-Object { $_.State -eq "Running" }).Count -ge $effectiveConcurrency) {
        $completed = Wait-Job -Job @($runningJobs) -Any
        $received = Receive-Job -Job $completed
        foreach ($item in @($received)) {
            $allResults.Add($item) | Out-Null
        }
        Remove-Job -Job $completed
        $completedIds = @($completed | ForEach-Object { $_.Id })
        $runningJobs = @($runningJobs | Where-Object { $completedIds -notcontains $_.Id })
    }

    $job = Start-Job -ScriptBlock $jobScript -ArgumentList @(
        $i,
        $endpoint,
        $path,
        $DeviceToken,
        $DeviceSecret,
        $Latitude,
        $Longitude,
        $BatteryLevel,
        $GpsAccuracy,
        [bool]$VerboseErrors
    )
    $runningJobs += $job

    if ($DelayMs -gt 0 -and $i -lt $Requests) {
        Start-Sleep -Milliseconds $DelayMs
    }
}

while ($runningJobs.Count -gt 0) {
    $completed = Wait-Job -Job @($runningJobs) -Any
    $received = Receive-Job -Job $completed
    foreach ($item in @($received)) {
        $allResults.Add($item) | Out-Null
    }
    Remove-Job -Job $completed
    $completedIds = @($completed | ForEach-Object { $_.Id })
    $runningJobs = @($runningJobs | Where-Object { $completedIds -notcontains $_.Id })
}

$stopwatchTotal.Stop()
$finishedAt = [DateTimeOffset]::UtcNow

$results = @($allResults.ToArray())
$successCount = @($results | Where-Object { $_.Success }).Count
$errorCount = @($results | Where-Object { -not $_.Success }).Count
$latencies = @($results | ForEach-Object { [double]$_.LatencyMs })
$durationMs = [Math]::Max(1, $stopwatchTotal.ElapsedMilliseconds)
$requestsPerSecond = [Math]::Round(($Requests * 1000.0) / $durationMs, 2)

if ($latencies.Count -gt 0) {
    $sortedLatencies = @($latencies | Sort-Object)
    $averageLatency = [Math]::Round((($latencies | Measure-Object -Average).Average), 2)
    $minLatency = [Math]::Round($sortedLatencies[0], 2)
    $maxLatency = [Math]::Round($sortedLatencies[$sortedLatencies.Count - 1], 2)
    $p95Index = [Math]::Ceiling($sortedLatencies.Count * 0.95) - 1
    $p95Index = [Math]::Max(0, [Math]::Min($p95Index, $sortedLatencies.Count - 1))
    $p95Latency = [Math]::Round($sortedLatencies[$p95Index], 2)
}
else {
    $averageLatency = 0
    $minLatency = 0
    $maxLatency = 0
    $p95Latency = 0
}

Write-Host ""
Write-Host "Status codes:"
$statusGroups = @($results | Group-Object -Property StatusCode | Sort-Object Name)
if ($statusGroups.Count -eq 0) {
    Write-Host "- none"
}
else {
    foreach ($group in $statusGroups) {
        Write-Host ("- {0}: {1}" -f $group.Name, $group.Count)
    }
}

if ($VerboseErrors -and $errorCount -gt 0) {
    Write-Host ""
    Write-Host "Error samples:"
    $results |
        Where-Object { -not $_.Success } |
        Select-Object -First 10 |
        ForEach-Object {
            Write-Host ("- request={0} status={1} latencyMs={2} error={3}" -f `
                    $_.RequestNumber,
                    $_.StatusCode,
                    $_.LatencyMs,
                    (Truncate-Text -Value $_.ErrorSummary -MaxLength 300))
        }
}

Write-Host ""
Write-Host "Summary:"
Write-Host "BaseUrl: $trimmedBaseUrl"
Write-Host "Endpoint: $path"
Write-Host "DeviceToken: $(Mask-Value -Value $DeviceToken)"
Write-Host "StartedAtUtc: $($startedAt.ToString("yyyy-MM-ddTHH:mm:ss'Z'"))"
Write-Host "FinishedAtUtc: $($finishedAt.ToString("yyyy-MM-ddTHH:mm:ss'Z'"))"
Write-Host "totalRequests: $Requests"
Write-Host "successCount: $successCount"
Write-Host "errorCount: $errorCount"
Write-Host "durationMs: $durationMs"
Write-Host "approximateRequestsPerSecond: $requestsPerSecond"
Write-Host "averageLatencyMs: $averageLatency"
Write-Host "minLatencyMs: $minLatency"
Write-Host "maxLatencyMs: $maxLatency"
Write-Host "p95LatencyMs: $p95Latency"

if ($errorCount -gt 0) {
    exit 1
}

exit 0
