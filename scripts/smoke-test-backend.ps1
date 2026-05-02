param(
    [Alias("BASE_URL")]
    [string]$BaseUrl = "http://localhost:8080",

    [Alias("EMAIL")]
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [Alias("PASSWORD")]
    [Parameter(Mandatory = $true)]
    [string]$Password
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Mask-Token {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Token
    )

    if ($Token.Length -le 12) {
        return ("*" * $Token.Length)
    }

    return "{0}...{1}" -f $Token.Substring(0, 6), $Token.Substring($Token.Length - 6)
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
$results = [ordered]@{
    Health = $false
    Login = $false
    Summary = $false
    TopPriorities = $false
    AiSummary = $false
}

Write-Host "Smoke test target: $trimmedBaseUrl"
Write-Host "User: $Email"
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

if ($null -ne $healthResult) {
    $results["Health"] = $true
    Write-StepResult -Name "Health" -Success $true -Detail "endpoint=$healthPath"
}
else {
    $failedHealth = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl$($healthEndpoints[0])"
    Write-StepResult -Name "Health" -Success $false -Detail (Format-HttpFailure -Result $failedHealth)
}

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
$maskedToken = Mask-Token -Token $token
Write-StepResult -Name "Login" -Success $true -Detail "token=$maskedToken"

$authHeaders = @{
    Authorization = "Bearer $token"
    "X-Request-Id" = [Guid]::NewGuid().ToString()
}

$summaryResult = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl/api/alert-analysis/summary" -Headers $authHeaders
if ($summaryResult.Ok) {
    $results["Summary"] = $true
    $summaryBody = $summaryResult.Body
    $detail = "riskLevel=$($summaryBody.riskLevel) pendingAlerts=$($summaryBody.totalPendingAlerts)"
    Write-StepResult -Name "Alert analysis summary" -Success $true -Detail $detail
}
else {
    Write-StepResult -Name "Alert analysis summary" -Success $false -Detail (Format-HttpFailure -Result $summaryResult)
}

$topPrioritiesResult = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl/api/alert-analysis/top-priorities?limit=5" -Headers $authHeaders
if ($topPrioritiesResult.Ok) {
    $results["TopPriorities"] = $true
    $priorityCount = 0
    if ($null -ne $topPrioritiesResult.Body) {
        $priorityCount = @($topPrioritiesResult.Body).Count
    }

    Write-StepResult -Name "Top priorities" -Success $true -Detail "items=$priorityCount"
}
else {
    Write-StepResult -Name "Top priorities" -Success $false -Detail (Format-HttpFailure -Result $topPrioritiesResult)
}

$aiSummaryResult = Invoke-JsonRequest -Method GET -Uri "$trimmedBaseUrl/api/alert-analysis/ai-summary" -Headers $authHeaders
if ($aiSummaryResult.Ok) {
    $results["AiSummary"] = $true
    $aiBody = $aiSummaryResult.Body
    $detail = "source=$($aiBody.source) fallbackUsed=$($aiBody.fallbackUsed) riskLevel=$($aiBody.riskLevel)"
    Write-StepResult -Name "AI summary" -Success $true -Detail $detail
}
else {
    Write-StepResult -Name "AI summary" -Success $false -Detail (Format-HttpFailure -Result $aiSummaryResult)
}

$allPassed = ($results.Values | Where-Object { -not $_ }).Count -eq 0

Write-Host ""
Write-Host "Final result: $(if ($allPassed) { 'PASS' } else { 'FAIL' })"

if (-not $allPassed) {
    exit 1
}
