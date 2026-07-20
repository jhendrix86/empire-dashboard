param(
    [ValidateSet('apk', 'exe', 'deb', 'run', 'all')]
    [string]$Target = 'run'
)

$gradlew = Join-Path $PSScriptRoot 'gradlew.bat'
if (-not (Test-Path $gradlew)) {
    Write-Error "gradlew.bat not found. Run: gradle wrapper first."
    exit 1
}

switch ($Target) {
    'apk' {
        Write-Host "Building Android APK..." -ForegroundColor Cyan
        & $gradlew ':composeApp:assembleRelease'
        Write-Host "APK output: composeApp\build\outputs\apk\release\" -ForegroundColor Green
    }
    'exe' {
        Write-Host "Building Windows EXE..." -ForegroundColor Cyan
        & $gradlew ':composeApp:packageExe'
        Write-Host "EXE output: composeApp\build\compose\binaries\main\exe\" -ForegroundColor Green
    }
    'deb' {
        Write-Host "Building Linux .deb (requires Linux or WSL)..." -ForegroundColor Yellow
        & $gradlew ':composeApp:packageDeb'
    }
    'run' {
        Write-Host "Launching desktop app..." -ForegroundColor Cyan
        & $gradlew ':composeApp:run'
    }
    'all' {
        Write-Host "Building all targets..." -ForegroundColor Cyan
        & $gradlew ':composeApp:assembleRelease' ':composeApp:packageExe'
    }
}
