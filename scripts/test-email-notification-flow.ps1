# Uso:
#   powershell -ExecutionPolicy Bypass -File .\scripts\test-email-notification-flow.ps1 `
#     -BaseUrl "http://localhost:8080" `
#     -Email "usuario@dominio.com" `
#     -Password "tu-password" `
#     -Mode "low-battery" `
#     -DeviceToken "COLLAR-001" `
#     -DeviceSecret "secret-del-dispositivo" `
#     -Latitude 1.214 `
#     -Longitude -77.281
#
#   powershell -ExecutionPolicy Bypass -File .\scripts\test-email-notification-flow.ps1 `
#     -BaseUrl "http://localhost:8080" `
#     -Email "usuario@dominio.com" `
#     -Password "tu-password" `
#     -Mode "geofence" `
#     -CollarToken "COLLAR-001" `
#     -Latitude 1.214 `
#     -Longitude -77.281

param(
    [Alias("BASE_URL")]
    [string]$BaseUrl = "http://localhost:8080",

    [Alias("EMAIL")]
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [Alias("PASSWORD")]
    [Parameter(Mandatory = $true)]
    [string]$Password,

    [ValidateSet("low-battery", "geofence", "offline")]
    [string]$Mode = "low-battery",

    [string]$CollarToken,
    [string]$DeviceToken,
    [string]$DeviceSecret,

    [double]$Latitude,
    [double]$Longitude,

    [int]$BatteryLevel = 10,
    [double]$GpsAccuracy = 5.0,
    [int]$PollAttempts = 4,
    [int]$PollDelaySeconds = 2
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

function Read-ErrorBody {
    param(
        [Parameter(Mandatory = $true)]
        [System.Exception]$Exception
    )

    $response = $Exception.Response
    if ($null -eq $response) {
        return $null
    }

    try {
        $stream = $response.GetResponseStream()
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

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("GET", "POST")]
        [string]$Method,

        [Parameter(Mandatory = $true)]
        [string]$Uri,

        [hashtable]$Headers,

        [object]$Body
    )

    try {
        $invokeParams = @{
            Method      = $Method
            Uri         = $Uri
            Headers     = $Headers
            ErrorAction = "Stop"
        }

        if ($null -ne $Body) {
            $invokeParams["ContentType"] = "application/json"
            $invokeParams["Body"] = ($Body | ConvertTo-Json -Depth 10 -Compress)
        }

        $response = Invoke-RestMethod @invokeParams

        return [pscustomobject]@{
            Ok         = $true
            StatusCode = 200
            Body       = $response
            ErrorBody  = $null
            ErrorText  = $null
        }
    }
    catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }
            catch {
                $statusCode = $null
            }
        }

        return [pscustomobject]@{
            Ok         = $false
            StatusCode = $statusCode
            Body       = $null
            ErrorBody  = (Read-ErrorBody -Exception $_.Exception)
            ErrorText  = $_.Exception.Message
        }
    }
}

function Invoke-RawJsonRequest {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("POST")]
        [string]$Method,

        [Parameter(Mandatory = $true)]
        [string]$Uri,

        [hashtable]$Headers,

        [Parameter(Mandatory = $true)]
        [string]$RawBody
    )

    try {
        $response = Invoke-RestMethod `
            -Method $Method `
            -Uri $Uri `
            -Headers $Headers `
            -ContentType "application/json" `
            -Body $RawBody `
            -ErrorAction Stop

        return [pscustomobject]@{
            Ok         = $true
            StatusCode = 200
            Body       = $response
            ErrorBody  = $null
            ErrorText  = $null
        }
    }
    catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }
            catch {
                $statusCode = $null
            }
        }

        return [pscustomobject]@{
            Ok         = $false
            StatusCode = $statusCode
            Body       = $null
            ErrorBody  = (Read-ErrorBody -Exception $_.Exception)
            ErrorText  = $_.Exception.Message
        }
    }
}

function Write-StepResult {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [bool]$Success,

        [string]$Detail
    )

    $status = if ($Success) { "OK" } else { "FAIL" }
    if ([string]::IsNullOrWhiteSpace($Detail)) {
        Write-Host ("{0}: {1}" -f $Name, $status)
        return
    }

    Write-Host ("{0}: {1} - {2}" -f $Name, $status, $Detail)
}

function Format-HttpFailure {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Result
    )

    $parts = @()

    if ($null -ne $Result.StatusCode) {
        $parts += "status=$($Result.StatusCode)"
    }

    if (-not [string]::IsNullOrWhiteSpace($Result.ErrorBody)) {
        $parts += "body=$($Result.ErrorBody)"
    }
    elseif (-not [string]::IsNullOrWhiteSpace($Result.ErrorText)) {
        $parts += "error=$($Result.ErrorText)"
    }

    if ($parts.Count -eq 0) {
        return "request failed"
    }

    return ($parts -join " | ")
}

function Get-IsoLocalTimestamp {
    return (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss")
}

function Get-IsoInstantTimestamp {
    return (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
}

function Sign-DeviceRequest {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Secret,

        [Parameter(Mandatory = $true)]
        [string]$TimestampHeader,

        [Parameter(Mandatory = $true)]
        [string]$Nonce,

        [Parameter(Mandatory = $true)]
        [string]$Body
    )

    $canonicalRequest = "POST`n/api/device/locations`n$TimestampHeader`n$Nonce`n$Body"
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([System.Text.Encoding]::UTF8.GetBytes($Secret))
    try {
        $signatureBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($canonicalRequest))
        return [System.Convert]::ToBase64String($signatureBytes)
    }
    finally {
        $hmac.Dispose()
    }
}

function Get-AlertsByType {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,

        [Parameter(Mandatory = $true)]
        [hashtable]$Headers,

        [Parameter(Mandatory = $true)]
        [string]$AlertType
    )

    $result = Invoke-JsonRequest -Method GET -Uri "$BaseUrl/api/alerts/type/$AlertType" -Headers $Headers
    if (-not $result.Ok) {
        return $result
    }

    $alerts = @()
    if ($null -ne $result.Body) {
        $alerts = @($result.Body)
    }

    return [pscustomobject]@{
        Ok = $true
        Alerts = $alerts
    }
}

function Get-PendingCountByType {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Alerts
    )

    return @($Alerts | Where-Object { $_.status -eq "PENDIENTE" }).Count
}

function Validate-ModeParameters {
    switch ($Mode) {
        "low-battery" {
            if ([string]::IsNullOrWhiteSpace($DeviceToken)) {
                throw "Mode=low-battery requiere -DeviceToken."
            }
            if ([string]::IsNullOrWhiteSpace($DeviceSecret)) {
                throw "Mode=low-battery requiere -DeviceSecret."
            }
            if ($PSBoundParameters.ContainsKey("Latitude") -eq $false -or $PSBoundParameters.ContainsKey("Longitude") -eq $false) {
                throw "Mode=low-battery requiere -Latitude y -Longitude."
            }
        }
        "geofence" {
            if ([string]::IsNullOrWhiteSpace($CollarToken)) {
                throw "Mode=geofence requiere -CollarToken."
            }
            if ($PSBoundParameters.ContainsKey("Latitude") -eq $false -or $PSBoundParameters.ContainsKey("Longitude") -eq $false) {
                throw "Mode=geofence requiere -Latitude y -Longitude."
            }
        }
        "offline" {
            throw "Mode=offline no se soporta en este script porque depende del scheduler y del paso del tiempo. Usa low-battery o geofence."
        }
    }
}

$trimmedBaseUrl = $BaseUrl.TrimEnd("/")
$results = [ordered]@{
    Health = $false
    Login = $false
    BeforeSummary = $false
    Trigger = $false
    AlertObserved = $false
    AfterSummary = $false
}

Validate-ModeParameters

Write-Host "Email notification flow target: $trimmedBaseUrl"
Write-Host "User: $Email"
Write-Host "Mode: $Mode"
if (-not [string]::IsNullOrWhiteSpace($CollarToken)) {
    Write-Host "CollarToken: $(Mask-Value -Value $CollarToken)"
}
if (-not [string]::IsNullOrWhiteSpace($DeviceToken)) {
    Write-Host "DeviceToken: $(Mask-Value -Value $DeviceToken)"
}
Write-Host ""

$healthEndpoints = @("/healthz", "/actuator/health")
$healthResult = $null
$healthPath = $null

foreach ($candidate in $healthEndpoints) {
    $attempt = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl$candidate"
    if ($attempt.Ok) {
        $healthResult = $attempt
        $healthPath = $candidate
        break
    }
}

if ($null -eq $healthResult) {
    $failedHealth = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl$($healthEndpoints[0])"
    Write-StepResult -Name "Health" -Success $false -Detail (Format-HttpFailure -Result $failedHealth)
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

$results["Health"] = $true
Write-StepResult -Name "Health" -Success $true -Detail "endpoint=$healthPath"

$loginBody = @{
    email = $Email
    password = $Password
}

$loginResult = Invoke-JsonRequest -Method POST -Uri "$trimmedBaseUrl/api/auth/login" -Body $loginBody
if (-not $loginResult.Ok) {
    Write-StepResult -Name "Login" -Success $false -Detail (Format-HttpFailure -Result $loginResult)
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

$token = $loginResult.Body.token
if ([string]::IsNullOrWhiteSpace($token)) {
    Write-StepResult -Name "Login" -Success $false -Detail "token missing in response"
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

$results["Login"] = $true
Write-StepResult -Name "Login" -Success $true -Detail "token=$(Mask-Value -Value $token)"

$authHeaders = @{
    Authorization = "Bearer $token"
    "X-Request-Id" = [Guid]::NewGuid().ToString()
}

$summaryBefore = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl/api/alert-analysis/summary" -Headers $authHeaders
if ($summaryBefore.Ok) {
    $results["BeforeSummary"] = $true
    Write-StepResult -Name "Summary before" -Success $true -Detail "riskLevel=$($summaryBefore.Body.riskLevel) pendingAlerts=$($summaryBefore.Body.totalPendingAlerts)"
}
else {
    Write-StepResult -Name "Summary before" -Success $false -Detail (Format-HttpFailure -Result $summaryBefore)
}

$expectedAlertType = if ($Mode -eq "geofence") { "EXIT_GEOFENCE" } else { "LOW_BATTERY" }
$alertsBeforeResult = Get-AlertsByType -BaseUrl $trimmedBaseUrl -Headers $authHeaders -AlertType $expectedAlertType
if (-not $alertsBeforeResult.Ok) {
    Write-StepResult -Name "Alerts before" -Success $false -Detail (Format-HttpFailure -Result $alertsBeforeResult)
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

$pendingBefore = Get-PendingCountByType -Alerts $alertsBeforeResult.Alerts
Write-StepResult -Name "Alerts before" -Success $true -Detail "type=$expectedAlertType pending=$pendingBefore"

if ($Mode -eq "geofence") {
    $locationBody = @{
        collarToken = $CollarToken
        latitude = $Latitude
        longitude = $Longitude
        timestamp = (Get-IsoLocalTimestamp)
    }

    $triggerResult = Invoke-JsonRequest -Method POST -Uri "$trimmedBaseUrl/api/locations" -Headers $authHeaders -Body $locationBody
    if (-not $triggerResult.Ok) {
        Write-StepResult -Name "Trigger geofence flow" -Success $false -Detail (Format-HttpFailure -Result $triggerResult)
        Write-Host ""
        Write-Host "Final result: FAIL"
        exit 1
    }

    $results["Trigger"] = $true
    Write-StepResult -Name "Trigger geofence flow" -Success $true -Detail "locationId=$($triggerResult.Body.id)"
}
elseif ($Mode -eq "low-battery") {
    $bodyObject = [ordered]@{
        latitude = $Latitude
        longitude = $Longitude
        timestamp = (Get-IsoLocalTimestamp)
        batteryLevel = $BatteryLevel
        gpsAccuracy = $GpsAccuracy
    }
    $rawBody = $bodyObject | ConvertTo-Json -Depth 5 -Compress
    $timestampHeader = Get-IsoInstantTimestamp
    $nonce = [Guid]::NewGuid().ToString()
    $signature = Sign-DeviceRequest -Secret $DeviceSecret -TimestampHeader $timestampHeader -Nonce $nonce -Body $rawBody
    $deviceHeaders = @{
        "X-Device-Token" = $DeviceToken
        "X-Device-Timestamp" = $timestampHeader
        "X-Device-Nonce" = $nonce
        "X-Device-Signature" = $signature
        "X-Request-Id" = [Guid]::NewGuid().ToString()
    }

    $triggerResult = Invoke-RawJsonRequest -Method POST -Uri "$trimmedBaseUrl/api/device/locations" -Headers $deviceHeaders -RawBody $rawBody
    if (-not $triggerResult.Ok) {
        Write-StepResult -Name "Trigger low-battery flow" -Success $false -Detail (Format-HttpFailure -Result $triggerResult)
        Write-Host ""
        Write-Host "Final result: FAIL"
        exit 1
    }

    $results["Trigger"] = $true
    Write-StepResult -Name "Trigger low-battery flow" -Success $true -Detail "locationId=$($triggerResult.Body.id)"
}

$observedAlert = $null
for ($attempt = 1; $attempt -le $PollAttempts; $attempt++) {
    $alertsAfterResult = Get-AlertsByType -BaseUrl $trimmedBaseUrl -Headers $authHeaders -AlertType $expectedAlertType
    if ($alertsAfterResult.Ok) {
        $pendingAfter = Get-PendingCountByType -Alerts $alertsAfterResult.Alerts
        if ($pendingAfter -gt $pendingBefore) {
            $observedAlert = @($alertsAfterResult.Alerts | Where-Object { $_.status -eq "PENDIENTE" } | Sort-Object createdAt -Descending)[0]
            break
        }
    }

    if ($attempt -lt $PollAttempts) {
        Start-Sleep -Seconds $PollDelaySeconds
    }
}

if ($null -ne $observedAlert) {
    $results["AlertObserved"] = $true
    Write-StepResult -Name "Alert observed" -Success $true -Detail "type=$($observedAlert.type) alertId=$($observedAlert.id) status=$($observedAlert.status)"
}
else {
    Write-StepResult -Name "Alert observed" -Success $false -Detail "No se observó incremento de alertas pendientes tipo $expectedAlertType. Puede existir anti-duplicado activo o precondición operativa no cumplida."
}

$summaryAfter = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl/api/alert-analysis/summary" -Headers $authHeaders
if ($summaryAfter.Ok) {
    $results["AfterSummary"] = $true
    Write-StepResult -Name "Summary after" -Success $true -Detail "riskLevel=$($summaryAfter.Body.riskLevel) pendingAlerts=$($summaryAfter.Body.totalPendingAlerts)"
}
else {
    Write-StepResult -Name "Summary after" -Success $false -Detail (Format-HttpFailure -Result $summaryAfter)
}

Write-Host ""
Write-Host "Revisión operativa sugerida:"
Write-Host "- Verifica inbox del destinatario configurado por preferencias o fallback global."
Write-Host "- Revisa logs con event=email_notification_sent."
Write-Host "- Si no llega correo, revisa event=email_notification_skipped y notification_dispatch_failed."
Write-Host "- Para geofence, confirma que el collar esté asociado a una vaca con geocerca activa y que las coordenadas queden fuera."
Write-Host "- Para low-battery, confirma que el DeviceToken y DeviceSecret correspondan a un collar activo."

$allPassed = ($results.Values | Where-Object { -not $_ }).Count -eq 0
Write-Host ""
Write-Host "Final result: $(if ($allPassed) { 'PASS' } else { 'FAIL' })"

if (-not $allPassed) {
    exit 1
}
