# Uso local:
#   powershell -ExecutionPolicy Bypass -File .\scripts\test-email-outbox-flow.ps1 `
#     -BaseUrl "http://localhost:8080" `
#     -AdminEmail "admin@ganaderia.com" `
#     -AdminPassword "tu-password" `
#     -ForgotPasswordEmail "admin@ganaderia.com" `
#     -WaitSeconds 10
#
# Requiere un usuario ADMINISTRADOR porque consulta el diagnostico admin del outbox.
#
# Para validar localmente el outbox EMAIL, iniciar backend con:
#   APP_NOTIFICATIONS_EMAIL_ENABLED=true
#   APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=outbox
#   APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=true
#   APP_NOTIFICATIONS_EMAIL_PROVIDER=resend
#   APP_NOTIFICATIONS_EMAIL_API_KEY=...
#   APP_NOTIFICATIONS_EMAIL_FROM=Ganaderia 4.0 <onboarding@resend.dev>
#   APP_FRONTEND_PASSWORD_RESET_URL=http://localhost:5173/reset-password
#
# En Render, produccion normal recomendada:
#   APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=direct
#   APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=false
#
# En Render, solo para prueba controlada despues de validar local:
#   APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=outbox
#   APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=true
#
# El script:
# - valida health
# - hace login
# - valida /api/auth/me
# - ejecuta forgot-password
# - espera a que el processor EMAIL outbox pueda correr
# - consulta /api/admin/notification-outbox?channel=EMAIL&page=0&size=10
# - no imprime JWT completo, reset token, secrets, payload, HTML/textBody ni password
# - no consulta DB ni llama Resend directamente

param(
    [Alias("BASE_URL")]
    [string]$BaseUrl = "http://localhost:8080",

    [Parameter(Mandatory = $true)]
    [string]$AdminEmail,

    [Parameter(Mandatory = $true)]
    [string]$AdminPassword,

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
            ErrorAction = "Stop"
        }

        if ($null -ne $Headers) {
            $invokeParams["Headers"] = $Headers
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

function Format-Nullable {
    param([object]$Value)

    if ($null -eq $Value) {
        return "-"
    }

    $stringValue = [string]$Value
    if ([string]::IsNullOrWhiteSpace($stringValue)) {
        return "-"
    }

    return $stringValue
}

function Get-PropertyValue {
    param(
        [object]$Object,
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if ($null -eq $Object) {
        return $null
    }

    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Get-OutboxContent {
    param([object]$Body)

    $content = Get-PropertyValue -Object $Body -Name "content"
    if ($null -eq $content) {
        return @()
    }

    return @($content)
}

function Write-OutboxMessages {
    param(
        [object[]]$Messages
    )

    if (@($Messages).Count -eq 0) {
        Write-Host "No EMAIL outbox messages found in recent page."
        return
    }

    Write-Host ""
    Write-Host "Outbox recent EMAIL messages:"
    $Messages |
        Select-Object `
            @{Name = "id"; Expression = { $_.id } },
            @{Name = "channel"; Expression = { $_.channel } },
            @{Name = "status"; Expression = { $_.status } },
            @{Name = "eventType"; Expression = { $_.eventType } },
            @{Name = "recipientMasked"; Expression = { $_.recipientMasked } },
            @{Name = "attempts"; Expression = { $_.attempts } },
            @{Name = "nextAttemptAt"; Expression = { Format-Nullable -Value $_.nextAttemptAt } },
            @{Name = "sentAt"; Expression = { Format-Nullable -Value $_.sentAt } },
            @{Name = "failedAt"; Expression = { Format-Nullable -Value $_.failedAt } },
            @{Name = "lastErrorSummary"; Expression = { Format-Nullable -Value $_.lastErrorSummary } } |
        Format-Table -AutoSize
}

$trimmedBaseUrl = $BaseUrl.TrimEnd("/")
$targetForgotPasswordEmail = if ([string]::IsNullOrWhiteSpace($ForgotPasswordEmail)) { $AdminEmail } else { $ForgotPasswordEmail.Trim() }
$maskedForgotPasswordEmail = Mask-Email -Value $targetForgotPasswordEmail

$results = [ordered]@{
    Health = $false
    Login = $false
    Me = $false
    ForgotPassword = $false
    Wait = $false
    Outbox = $false
}

Write-Host "Email outbox flow target: $trimmedBaseUrl"
Write-Host "Login user: $(Mask-Email -Value $AdminEmail)"
Write-Host "Forgot-password target: $maskedForgotPasswordEmail"
Write-Host "WaitSeconds: $WaitSeconds"
Write-Host ""
Write-Host "Precondiciones requeridas en backend local:"
Write-Host "- APP_NOTIFICATIONS_EMAIL_ENABLED=true"
Write-Host "- APP_NOTIFICATIONS_EMAIL_DELIVERY_MODE=outbox"
Write-Host "- APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=true"
Write-Host "- APP_NOTIFICATIONS_EMAIL_PROVIDER=resend"
Write-Host "- APP_NOTIFICATIONS_EMAIL_API_KEY=<configured>"
Write-Host "- APP_NOTIFICATIONS_EMAIL_FROM=Ganaderia 4.0 <onboarding@resend.dev>"
Write-Host "- APP_FRONTEND_PASSWORD_RESET_URL=http://localhost:5173/reset-password"
Write-Host ""
Write-Host "Render:"
Write-Host "- Produccion normal: delivery-mode=direct y processor=false"
Write-Host "- Prueba controlada: delivery-mode=outbox y processor=true, despues de probar localmente"
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
    email = $AdminEmail
    password = $AdminPassword
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
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
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

$outboxUri = "$trimmedBaseUrl/api/admin/notification-outbox?channel=EMAIL&page=0&size=10"
$outboxResult = Invoke-JsonRequest -Method GET -Uri $outboxUri -Headers $authHeaders
if (-not $outboxResult.Ok) {
    Write-StepResult -Name "Outbox admin query" -Success $false -Detail (Format-HttpFailure -Result $outboxResult)
    Write-Host ""
    Write-Host "El endpoint requiere rol ADMINISTRADOR. No se imprime payload ni se consulta DB."
    Write-Host "Final result: FAIL"
    exit 1
}

$outbox = $outboxResult.Body
$contentProperty = if ($null -eq $outbox) { $null } else { $outbox.PSObject.Properties["content"] }
if ($null -eq $contentProperty) {
    Write-StepResult -Name "Outbox admin query" -Success $false -Detail "response missing content property"
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

$messages = @(Get-OutboxContent -Body $outbox)
$page = Format-Nullable -Value (Get-PropertyValue -Object $outbox -Name "page")
$size = Format-Nullable -Value (Get-PropertyValue -Object $outbox -Name "size")
$totalElements = Format-Nullable -Value (Get-PropertyValue -Object $outbox -Name "totalElements")
$totalPages = Format-Nullable -Value (Get-PropertyValue -Object $outbox -Name "totalPages")
$numberOfElements = Format-Nullable -Value (Get-PropertyValue -Object $outbox -Name "numberOfElements")

$results["Outbox"] = $true
Write-StepResult -Name "Outbox admin query" -Success $true -Detail "items=$($messages.Count) page=$page size=$size totalElements=$totalElements totalPages=$totalPages numberOfElements=$numberOfElements"

if ($messages.Count -eq 0) {
    Write-Host "No EMAIL outbox messages found in recent page."
    Write-Host ""
    Write-Host "Posibles causas:"
    Write-Host "1. backend no esta en delivery-mode=outbox"
    Write-Host "2. processor apagado"
    Write-Host "3. flujo usado no encola en notification_outbox"
    Write-Host "4. usuario inexistente/inactivo"
    Write-Host "5. email disabled/missing config"
    Write-Host ""
    Write-Host "Final result: FAIL"
    exit 1
}

Write-OutboxMessages -Messages $messages

$sentMessages = @($messages | Where-Object { $_.status -eq "SENT" })
$pendingMessages = @($messages | Where-Object { $_.status -eq "PENDING" })
$failedMessages = @($messages | Where-Object { $_.status -eq "FAILED" })
$deadMessages = @($messages | Where-Object { $_.status -eq "DEAD" })

Write-Host ""
if ($sentMessages.Count -gt 0) {
    Write-StepResult -Name "Outbox SENT check" -Success $true -Detail "sentMessagesInPage=$($sentMessages.Count)"
}
else {
    Write-StepResult -Name "Outbox SENT check" -Success $false -Detail "no SENT message found in the latest EMAIL page"
}

Write-Host ""
Write-Host "Que revisar si no aparece SENT:"
Write-Host "- PENDING: confirma APP_NOTIFICATIONS_OUTBOX_EMAIL_PROCESSOR_ENABLED=true y delivery-mode=outbox."
Write-Host "- FAILED: revisa lastErrorSummary y logs event=notification_outbox_email_send_failed."
Write-Host "- DEAD: revisa lastErrorSummary, maxAttempts y usa requeue admin solo si corresponde."
Write-Host "- Sin mensajes: confirma que el email exista como usuario activo y que EMAIL este habilitado."
Write-Host "- Logs utiles: email_notification_enqueued_for_outbox, notification_outbox_email_processor_completed."
Write-Host "- Correo: valida llegada del email de recuperacion en el destinatario."
Write-Host "- Este script no imprime reset token, payload, HTML/textBody, API key, secrets ni password."

$allCorePassed = ($results.Values | Where-Object { -not $_ }).Count -eq 0
$sentFound = $sentMessages.Count -gt 0
$allPassed = $allCorePassed -and $sentFound

Write-Host ""
Write-Host "Outbox status counts: SENT=$($sentMessages.Count) PENDING=$($pendingMessages.Count) FAILED=$($failedMessages.Count) DEAD=$($deadMessages.Count)"
Write-Host "Final result: $(if ($allPassed) { 'PASS' } else { 'CHECK_OUTBOX_STATUS' })"

if (-not $allPassed) {
    exit 1
}
