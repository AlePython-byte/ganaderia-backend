param(
    [Alias("BASE_URL")]
    [string]$BaseUrl = "http://localhost:8080",

    [Alias("DEVICE_TOKEN")]
    [Parameter(Mandatory = $true)]
    [string]$DeviceToken,

    [Alias("DEVICE_SECRET")]
    [Parameter(Mandatory = $true)]
    [string]$DeviceSecret,

    [Alias("latitude")]
    [Parameter(Mandatory = $true)]
    [double]$Latitude,

    [Alias("longitude")]
    [Parameter(Mandatory = $true)]
    [double]$Longitude,

    [Alias("batteryLevel")]
    [Nullable[int]]$BatteryLevel,

    [Alias("gpsAccuracy")]
    [Nullable[double]]$GpsAccuracy
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

$path = "/api/device/locations"
$trimmedBaseUrl = $BaseUrl.TrimEnd("/")

# Header timestamp:
# - must be ISO-8601 UTC with Z and is validated by DeviceRequestAuthenticationService as Instant.
$requestInstantUtc = [DateTimeOffset]::UtcNow
$timestampHeader = $requestInstantUtc.ToString("yyyy-MM-ddTHH:mm:ss'Z'")

# Body timestamp:
# - DeviceLocationRequestDTO expects yyyy-MM-ddTHH:mm:ss without offset because it uses LocalDateTime.
# - To avoid clock interpretation drift between Windows local time, Docker and Render,
#   the script uses the same UTC instant as the auth timestamp, but without the trailing Z.
$bodyTimestamp = $requestInstantUtc.ToString("yyyy-MM-ddTHH:mm:ss")

$nonce = [Guid]::NewGuid().ToString()

# Build the JSON body exactly with the fields the backend currently accepts.
$bodyObject = [ordered]@{
    latitude  = $Latitude
    longitude = $Longitude
    timestamp = $bodyTimestamp
}

if ($null -ne $BatteryLevel) {
    $bodyObject["batteryLevel"] = $BatteryLevel
}

if ($null -ne $GpsAccuracy) {
    $bodyObject["gpsAccuracy"] = $GpsAccuracy
}

$bodyJson = $bodyObject | ConvertTo-Json -Compress

# Canonical request format from DeviceRequestAuthenticationService:
# METHOD + "\n" + PATH + "\n" + TIMESTAMP_HEADER + "\n" + NONCE + "\n" + RAW_BODY
$canonicalRequest = "POST`n$path`n$timestampHeader`n$nonce`n$bodyJson"

$hmac = [System.Security.Cryptography.HMACSHA256]::new([System.Text.Encoding]::UTF8.GetBytes($DeviceSecret))
try {
    $signatureBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($canonicalRequest))
}
finally {
    $hmac.Dispose()
}

$signature = [Convert]::ToBase64String($signatureBytes)

$headers = @{
    "X-Device-Token"     = $DeviceToken
    "X-Device-Timestamp" = $timestampHeader
    "X-Device-Nonce"     = $nonce
    "X-Device-Signature" = $signature
}

Write-Host "Sending POST $trimmedBaseUrl$path"
Write-Host "Device token: $(Mask-Value -Value $DeviceToken)"
Write-Host "UTC auth timestamp: $timestampHeader"
Write-Host "Body timestamp: $bodyTimestamp"
Write-Host "Nonce: $(Mask-Value -Value $nonce)"
Write-Host "Body: $bodyJson"

if ($null -ne $BatteryLevel -or $null -ne $GpsAccuracy) {
    if ($null -ne $BatteryLevel) {
        Write-Host "BatteryLevel (sent): $BatteryLevel"
    }
    if ($null -ne $GpsAccuracy) {
        Write-Host "GpsAccuracy (sent): $GpsAccuracy"
    }
}

try {
    $response = Invoke-RestMethod `
        -Method Post `
        -Uri "$trimmedBaseUrl$path" `
        -Headers $headers `
        -ContentType "application/json" `
        -Body $bodyJson

    Write-Host ""
    Write-Host "Server response:"
    $response | ConvertTo-Json -Depth 10
}
catch {
    Write-Host ""
    Write-Host "Request failed."

    $exception = $_.Exception
    if ($exception.Response -and $exception.Response.GetResponseStream()) {
        $reader = [System.IO.StreamReader]::new($exception.Response.GetResponseStream())
        try {
            $responseBody = $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }

        if ($responseBody) {
            Write-Host "Response body:"
            Write-Host $responseBody
        }
    }

    throw
}
