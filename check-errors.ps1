# Periodic error checking script for Empire Dashboard
# Run this periodically to catch any issues with the API server or app

param(
    [int]$IntervalSeconds = 300  # Check every 5 minutes by default
)

function Check-ApiServerErrors {
    $apiFile = 'C:\Users\LXGIXN\OneDrive\Copilot\Launch Assets - CEO Prompt Vault\empire-api-server.ps1'
    
    # Check file exists
    if (-not (Test-Path $apiFile)) {
        return @{ status = 'ERROR'; message = 'API server file not found' }
    }
    
    # Check syntax
    try {
        [ScriptBlock]::Create((Get-Content $apiFile -Raw)) | Out-Null
        return @{ status = 'OK'; message = 'API server syntax valid' }
    }
    catch {
        return @{ status = 'ERROR'; message = "Syntax error: $($_.Exception.Message)" }
    }
}

function Check-AppProjectErrors {
    $projectRoot = 'C:\Users\LXGIXN\EmpireDashboard'
    
    if (-not (Test-Path $projectRoot)) {
        return @{ status = 'ERROR'; message = 'App project directory not found' }
    }
    
    # Check key files exist
    $requiredFiles = @(
        'build.gradle.kts',
        'settings.gradle.kts',
        'composeApp/build.gradle.kts',
        'composeApp/src/commonMain/kotlin/App.kt'
    )
    
    foreach ($file in $requiredFiles) {
        $fullPath = Join-Path $projectRoot $file
        if (-not (Test-Path $fullPath)) {
            return @{ status = 'WARN'; message = "Missing file: $file" }
        }
    }
    
    return @{ status = 'OK'; message = 'App project structure intact' }
}

function Check-GitRepository {
    $repoPath = 'C:\Users\LXGIXN\EmpireDashboard'
    
    if (-not (Test-Path "$repoPath\.git")) {
        return @{ status = 'WARN'; message = 'Git repository not initialized' }
    }
    
    try {
        $status = & git -C $repoPath status --porcelain 2>&1
        $uncommitted = ($status | Measure-Object -Line).Lines
        
        if ($uncommitted -gt 0) {
            return @{ status = 'WARN'; message = "Uncommitted changes: $uncommitted files" }
        }
        
        return @{ status = 'OK'; message = 'Repository clean' }
    }
    catch {
        return @{ status = 'ERROR'; message = "Git check failed: $_" }
    }
}

function Check-DataIntegrity {
    $dataDir = 'C:\Users\LXGIXN\OneDrive\Copilot\Launch Assets - CEO Prompt Vault\empire-data'
    
    if (-not (Test-Path $dataDir)) {
        return @{ status = 'INFO'; message = 'Data directory not created yet' }
    }
    
    $files = @('customers.json', 'leads.json', 'revenue.json')
    $issues = @()
    
    foreach ($file in $files) {
        $path = Join-Path $dataDir $file
        if (Test-Path $path) {
            try {
                Get-Content $path -Raw | ConvertFrom-Json | Out-Null
            }
            catch {
                $issues += "Invalid JSON: $file"
            }
        }
    }
    
    if ($issues.Count -gt 0) {
        return @{ status = 'ERROR'; message = ($issues -join '; ') }
    }
    
    return @{ status = 'OK'; message = 'Data files valid' }
}

function Write-ErrorReport {
    param([hashtable]$Report)
    
    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    $color = switch ($Report.status) {
        'OK' { 'Green' }
        'WARN' { 'Yellow' }
        'ERROR' { 'Red' }
        'INFO' { 'Cyan' }
        default { 'White' }
    }
    
    Write-Host "[$timestamp] [$($Report.status)] $($Report.message)" -ForegroundColor $color
}

# Main loop
Write-Host "=== Empire Dashboard Error Checker ===" -ForegroundColor Cyan
Write-Host "Checking every $IntervalSeconds seconds. Press Ctrl+C to stop." -ForegroundColor Gray

while ($true) {
    $timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    Write-Host "`n[$timestamp] Running error checks..." -ForegroundColor Cyan
    
    Write-ErrorReport (Check-ApiServerErrors)
    Write-ErrorReport (Check-AppProjectErrors)
    Write-ErrorReport (Check-GitRepository)
    Write-ErrorReport (Check-DataIntegrity)
    
    Write-Host "Next check in $IntervalSeconds seconds..." -ForegroundColor Gray
    Start-Sleep -Seconds $IntervalSeconds
}
