# Uso:
#   powershell -ExecutionPolicy Bypass -File .\scripts\seed-demo-data.ps1 `
#     -BaseUrl "http://localhost:8080" `
#     -Email "admin@ganaderia.com" `
#     -Password "tu-password"
#
# El script:
# - hace login via JWT
# - crea vacas y collares demo usando la API
# - puede crear geocerca, ubicaciones manuales y preferencias de notificacion
# - no inserta SQL directo
# - no imprime JWT completo ni secretos

param(
    [Alias("BASE_URL")]
    [string]$BaseUrl = "http://localhost:8080",

    [Alias("EMAIL")]
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [Alias("PASSWORD")]
    [Parameter(Mandatory = $true)]
    [string]$Password,

    [string]$Prefix = "Demo",
    [bool]$SkipExisting = $true,
    [bool]$IncludeEmailPreferences = $true,
    [bool]$IncludeGeofence = $true
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
        [ValidateSet("GET", "POST", "PUT")]
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

function New-RequestHeaders {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Jwt
    )

    return @{
        Authorization = "Bearer $Jwt"
        "X-Request-Id" = [Guid]::NewGuid().ToString()
    }
}

function Get-UniqueName {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseName
    )

    $suffix = (Get-Date).ToString("MMddHHmmss")
    return "$BaseName $suffix"
}

function Get-ExistingCowByName {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$Jwt,
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $result = Invoke-JsonRequest -Method GET -Uri "$BaseUrl/api/cows" -Headers (New-RequestHeaders -Jwt $Jwt)
    if (-not $result.Ok) {
        throw "No fue posible consultar vacas existentes: $(Format-HttpFailure -Result $result)"
    }

    return @($result.Body) | Where-Object { $_.name -eq $Name } | Select-Object -First 1
}

function Ensure-Cow {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$Jwt,
        [Parameter(Mandatory = $true)]
        [string]$DesiredName
    )

    $targetName = $DesiredName
    if ($SkipExisting) {
        $existing = Get-ExistingCowByName -BaseUrl $BaseUrl -Jwt $Jwt -Name $DesiredName
        if ($null -ne $existing) {
            Write-StepResult -Name "Cow $DesiredName" -Success $true -Detail "reused id=$($existing.id) token=$($existing.token)"
            return $existing
        }
    }
    else {
        $targetName = Get-UniqueName -BaseName $DesiredName
    }

    $body = @{
        name = $targetName
        status = "ACTIVA"
        observations = "Seed demo por API"
    }

    $result = Invoke-JsonRequest -Method POST -Uri "$BaseUrl/api/cows" -Headers (New-RequestHeaders -Jwt $Jwt) -Body $body
    if (-not $result.Ok) {
        throw "No fue posible crear vaca '$targetName': $(Format-HttpFailure -Result $result)"
    }

    Write-StepResult -Name "Cow $targetName" -Success $true -Detail "created id=$($result.Body.id) token=$($result.Body.token)"
    return $result.Body
}

function Get-ExistingCollarByCowId {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$Jwt,
        [Parameter(Mandatory = $true)]
        [long]$CowId
    )

    $result = Invoke-JsonRequest -Method GET -Uri "$BaseUrl/api/collars" -Headers (New-RequestHeaders -Jwt $Jwt)
    if (-not $result.Ok) {
        throw "No fue posible consultar collares existentes: $(Format-HttpFailure -Result $result)"
    }

    return @($result.Body) | Where-Object { $_.cowId -eq $CowId } | Select-Object -First 1
}

function Ensure-Collar {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$Jwt,
        [Parameter(Mandatory = $true)]
        [object]$Cow
    )

    if ($SkipExisting) {
        $existing = Get-ExistingCollarByCowId -BaseUrl $BaseUrl -Jwt $Jwt -CowId $Cow.id
        if ($null -ne $existing) {
            Write-StepResult -Name "Collar for $($Cow.name)" -Success $true -Detail "reused id=$($existing.id) token=$($existing.token)"
            return $existing
        }
    }

    $body = @{
        status = "ACTIVE"
        cowId = $Cow.id
        batteryLevel = 100
        signalStatus = "ONLINE"
        enabled = $true
        notes = "Seed demo por API"
    }

    $result = Invoke-JsonRequest -Method POST -Uri "$BaseUrl/api/collars" -Headers (New-RequestHeaders -Jwt $Jwt) -Body $body
    if (-not $result.Ok) {
        throw "No fue posible crear collar para '$($Cow.name)': $(Format-HttpFailure -Result $result)"
    }

    Write-StepResult -Name "Collar for $($Cow.name)" -Success $true -Detail "created id=$($result.Body.id) token=$($result.Body.token)"
    return $result.Body
}

function Get-ExistingGeofenceByName {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$Jwt,
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $result = Invoke-JsonRequest -Method GET -Uri "$BaseUrl/api/geofences" -Headers (New-RequestHeaders -Jwt $Jwt)
    if (-not $result.Ok) {
        throw "No fue posible consultar geocercas existentes: $(Format-HttpFailure -Result $result)"
    }

    return @($result.Body) | Where-Object { $_.name -eq $Name } | Select-Object -First 1
}

function Ensure-Geofence {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$Jwt,
        [Parameter(Mandatory = $true)]
        [object]$Cow
    )

    $desiredName = "$Prefix Geofence $($Cow.name)"
    $targetName = $desiredName

    if ($SkipExisting) {
        $existing = Get-ExistingGeofenceByName -BaseUrl $BaseUrl -Jwt $Jwt -Name $desiredName
        if ($null -ne $existing) {
            Write-StepResult -Name "Geofence $desiredName" -Success $true -Detail "reused id=$($existing.id) active=$($existing.active)"
            return $existing
        }
    }
    else {
        $targetName = Get-UniqueName -BaseName $desiredName
    }

    $body = @{
        name = $targetName
        centerLatitude = 1.214
        centerLongitude = -77.281
        radiusMeters = 250.0
        active = $true
        cowId = $Cow.id
    }

    $result = Invoke-JsonRequest -Method POST -Uri "$BaseUrl/api/geofences" -Headers (New-RequestHeaders -Jwt $Jwt) -Body $body
    if (-not $result.Ok) {
        throw "No fue posible crear geocerca '$targetName': $(Format-HttpFailure -Result $result)"
    }

    Write-StepResult -Name "Geofence $targetName" -Success $true -Detail "created id=$($result.Body.id)"
    return $result.Body
}

function Create-ManualLocation {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$Jwt,
        [Parameter(Mandatory = $true)]
        [string]$CollarToken,
        [Parameter(Mandatory = $true)]
        [double]$Latitude,
        [Parameter(Mandatory = $true)]
        [double]$Longitude
    )

    $body = @{
        collarToken = $CollarToken
        latitude = $Latitude
        longitude = $Longitude
        timestamp = Get-IsoLocalTimestamp
    }

    $result = Invoke-JsonRequest -Method POST -Uri "$BaseUrl/api/locations" -Headers (New-RequestHeaders -Jwt $Jwt) -Body $body
    if (-not $result.Ok) {
        throw "No fue posible crear ubicacion para collar '$CollarToken': $(Format-HttpFailure -Result $result)"
    }

    Write-StepResult -Name "Location $CollarToken" -Success $true -Detail "created id=$($result.Body.id)"
    return $result.Body
}

function Ensure-NotificationPreferences {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$Jwt
    )

    $meResult = Invoke-JsonRequest -Method GET -Uri "$BaseUrl/api/auth/me" -Headers (New-RequestHeaders -Jwt $Jwt)
    if (-not $meResult.Ok) {
        throw "No fue posible obtener usuario autenticado: $(Format-HttpFailure -Result $meResult)"
    }

    $userId = $meResult.Body.id
    $body = @{
        emailEnabled = $true
        smsEnabled = $false
        notificationEmail = $meResult.Body.email
        phoneNumber = $null
        minimumSeverity = "MEDIUM"
    }

    $result = Invoke-JsonRequest -Method PUT -Uri "$BaseUrl/api/users/$userId/notification-preferences" -Headers (New-RequestHeaders -Jwt $Jwt) -Body $body
    if (-not $result.Ok) {
        throw "No fue posible configurar preferencias de notificacion: $(Format-HttpFailure -Result $result)"
    }

    Write-StepResult -Name "Notification preferences" -Success $true -Detail "userId=$userId minimumSeverity=$($result.Body.minimumSeverity)"
    return $result.Body
}

$trimmedBaseUrl = $BaseUrl.TrimEnd("/")
$summary = [ordered]@{
    cows = @()
    collars = @()
    geofence = $null
    locations = @()
    notificationPreferences = $null
}

Write-Host "Demo seed target: $trimmedBaseUrl"
Write-Host "User: $Email"
Write-Host "Prefix: $Prefix"
Write-Host "SkipExisting: $SkipExisting"
Write-Host ""

$health = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl/healthz"
if (-not $health.Ok) {
    Write-StepResult -Name "Health" -Success $false -Detail (Format-HttpFailure -Result $health)
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}
Write-StepResult -Name "Health" -Success $true -Detail "endpoint=/healthz"

$loginBody = @{
    email = $Email
    password = $Password
}

$login = Invoke-JsonRequest -Method POST -Uri "$trimmedBaseUrl/api/auth/login" -Body $loginBody
if (-not $login.Ok) {
    Write-StepResult -Name "Login" -Success $false -Detail (Format-HttpFailure -Result $login)
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

$jwt = $login.Body.token
if ([string]::IsNullOrWhiteSpace($jwt)) {
    Write-StepResult -Name "Login" -Success $false -Detail "token missing in response"
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

Write-StepResult -Name "Login" -Success $true -Detail "token=$(Mask-Value -Value $jwt)"

try {
    $cowOne = Ensure-Cow -BaseUrl $trimmedBaseUrl -Jwt $jwt -DesiredName "$Prefix Luna"
    $cowTwo = Ensure-Cow -BaseUrl $trimmedBaseUrl -Jwt $jwt -DesiredName "$Prefix Estrella"
    $summary.cows += @($cowOne, $cowTwo)

    $collarOne = Ensure-Collar -BaseUrl $trimmedBaseUrl -Jwt $jwt -Cow $cowOne
    $collarTwo = Ensure-Collar -BaseUrl $trimmedBaseUrl -Jwt $jwt -Cow $cowTwo
    $summary.collars += @($collarOne, $collarTwo)

    if ($IncludeGeofence) {
        $geofence = Ensure-Geofence -BaseUrl $trimmedBaseUrl -Jwt $jwt -Cow $cowOne
        $summary.geofence = $geofence
    }
    else {
        Write-StepResult -Name "Geofence" -Success $true -Detail "skipped by flag"
    }

    $locationOne = Create-ManualLocation -BaseUrl $trimmedBaseUrl -Jwt $jwt -CollarToken $collarOne.token -Latitude 1.2140 -Longitude -77.2810
    $locationTwo = Create-ManualLocation -BaseUrl $trimmedBaseUrl -Jwt $jwt -CollarToken $collarTwo.token -Latitude 1.2185 -Longitude -77.2765
    $summary.locations += @($locationOne, $locationTwo)

    if ($IncludeEmailPreferences) {
        $preferences = Ensure-NotificationPreferences -BaseUrl $trimmedBaseUrl -Jwt $jwt
        $summary.notificationPreferences = $preferences
    }
    else {
        Write-StepResult -Name "Notification preferences" -Success $true -Detail "skipped by flag"
    }
}
catch {
    Write-Host ""
    Write-Host "Seed failed."
    Write-Host $_.Exception.Message
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

Write-Host ""
Write-Host "Summary:"
Write-Host "- Cows:"
foreach ($cow in $summary.cows) {
    Write-Host ("  - id={0} token={1} name={2}" -f $cow.id, $cow.token, $cow.name)
}

Write-Host "- Collars:"
foreach ($collar in $summary.collars) {
    Write-Host ("  - id={0} token={1} cowId={2}" -f $collar.id, $collar.token, $collar.cowId)
}

if ($null -ne $summary.geofence) {
    Write-Host ("- Geofence: id={0} name={1} cowId={2}" -f $summary.geofence.id, $summary.geofence.name, $summary.geofence.cowId)
}
else {
    Write-Host "- Geofence: skipped"
}

Write-Host "- Locations:"
foreach ($location in $summary.locations) {
    Write-Host ("  - id={0} collarToken={1} cowToken={2}" -f $location.id, $location.collarToken, $location.cowToken)
}

if ($null -ne $summary.notificationPreferences) {
    Write-Host ("- Notification preferences: userId={0} emailEnabled={1} minimumSeverity={2}" -f $summary.notificationPreferences.userId, $summary.notificationPreferences.emailEnabled, $summary.notificationPreferences.minimumSeverity)
}
else {
    Write-Host "- Notification preferences: skipped"
}

Write-Host ""
Write-Host "Final result: PASS"
