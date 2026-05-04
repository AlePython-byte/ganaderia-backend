# Uso:
#   powershell -ExecutionPolicy Bypass -File .\scripts\test-email-outbox-flow.ps1 `
#     -BaseUrl "http://localhost:8080" `
#     -Email "admin@ganaderia.com" `
#     -Password "tu-password" `
#     -ForgotPasswordEmail "admin@ganaderia.com" `
#     -WaitSeconds 10
#
# Requiere que el backend se inicie con:
#   APP_NOTIFICATIONS_EMAIL_ENABLED=true
#   APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=outbox
#   APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=true
#
# El script:
# - valida health
# - hace login
# - ejecuta forgot-password
# - espera a que el processor EMAIL outbox pueda correr
# - no imprime JWT completo ni token de recuperación

param(
    [Alias("BASE_URL")]
    [string]$BaseUrl = "http://localhost:8080",

    [Alias("EMAIL")]
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [Alias("PASSWORD")]
    [Parameter(Mandatory = $true)]
    [string]$Password,

    [string]$ForgotPasswordEmail,

    [ValidateRange(1, 120)]
    [int]$WaitSeconds = 10
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

function Mask-Email {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    $parts = $Value.Split("@", 2)
    if ($parts.Count -ne 2) {
        return (Mask-Value -Value $Value)
    }

    $local = $parts[0]
    $domain = $parts[1]
    if ($local.Length -le 2) {
        return ("*" * $local.Length) + "@$domain"
    }

    return "{0}***@{1}" -f $local.Substring(0, 1), $domain
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

$trimmedBaseUrl = $BaseUrl.TrimEnd("/")
$targetForgotPasswordEmail = if ([string]::IsNullOrWhiteSpace($ForgotPasswordEmail)) { $Email } else { $ForgotPasswordEmail.Trim() }

$results = [ordered]@{
    Health = $false
    Login = $false
    Me = $false
    ForgotPassword = $false
    Wait = $false
}

Write-Host "Email outbox flow target: $trimmedBaseUrl"
Write-Host "Login user: $Email"
Write-Host "Forgot-password target: $(Mask-Email -Value $targetForgotPasswordEmail)"
Write-Host "WaitSeconds: $WaitSeconds"
Write-Host ""
Write-Host "Precondiciones requeridas en backend:"
Write-Host "- APP_NOTIFICATIONS_EMAIL_ENABLED=true"
Write-Host "- APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=outbox"
Write-Host "- APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=true"
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

$meResult = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl/api/auth/me" -Headers $authHeaders
if ($meResult.Ok) {
    $results["Me"] = $true
    Write-StepResult -Name "Auth me" -Success $true -Detail "userId=$($meResult.Body.id) role=$($meResult.Body.role)"
}
else {
    Write-StepResult -Name "Auth me" -Success $false -Detail (Format-HttpFailure -Result $meResult)
}

$forgotPasswordBody = @{
    email = $targetForgotPasswordEmail
}

$forgotPasswordResult = Invoke-JsonRequest -Method POST -Uri "$trimmedBaseUrl/api/auth/forgot-password" -Body $forgotPasswordBody
if (-not $forgotPasswordResult.Ok) {
    Write-StepResult -Name "Forgot password" -Success $false -Detail (Format-HttpFailure -Result $forgotPasswordResult)
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

$publicMessage = $null
if ($null -ne $forgotPasswordResult.Body) {
    $publicMessage = $forgotPasswordResult.Body.message
}

$results["ForgotPassword"] = $true
Write-StepResult -Name "Forgot password" -Success $true -Detail "genericMessage=$publicMessage"

Start-Sleep -Seconds $WaitSeconds
$results["Wait"] = $true
Write-StepResult -Name "Wait for processor" -Success $true -Detail "seconds=$WaitSeconds"

Write-Host ""
Write-Host "Revisión operativa sugerida:"
Write-Host "- Verifica que haya llegado el correo de recuperación al destinatario."
Write-Host "- Revisa logs con event=notification_outbox_email_processor_completed."
Write-Host "- Revisa logs con event=email_notification_enqueued_for_outbox."
Write-Host "- Si el provider procesa correctamente, revisa logs con event=notification_outbox_email_processor_completed sent>0."
Write-Host "- Si falla el envío, revisa event=notification_outbox_email_send_failed o notification_outbox_email_dead."
Write-Host "- Si quieres confirmación persistente, revisa la tabla notification_outbox y verifica status=SENT para el mensaje EMAIL correspondiente."
Write-Host "- Este script no consulta DB ni imprime reset token."

$allPassed = ($results.Values | Where-Object { -not $_ }).Count -eq 0
Write-Host ""
Write-Host "Final result: $(if ($allPassed) { 'PASS' } else { 'FAIL' })"

if (-not $allPassed) {
    exit 1
}
