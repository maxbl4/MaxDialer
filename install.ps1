<#
.SYNOPSIS
    Build MaxDialer, install it on a connected device and start it.

.DESCRIPTION
    Sets JAVA_HOME to Android Studio's bundled JBR, builds the release APK (or debug with -DebugBuild),
    installs it with adb, compiles it ahead of time so scrolling is smooth from the first launch,
    and starts the app. The release build is signed with the release key plus the rotation
    proof from the debug key (sign-release.ps1), so release and debug builds install over each
    other and keep granted permissions.

.PARAMETER Serial
    adb serial of the target device. Optional when exactly one device/emulator is connected.

.PARAMETER DebugBuild
    Build and install the debug build. Compose is several times slower there: lists stutter.

.EXAMPLE
    ./install.ps1

.EXAMPLE
    ./install.ps1 -DebugBuild
#>
param(
    [string]$Serial,
    [switch]$DebugBuild
)

$ErrorActionPreference = 'Stop'
$projectDir = $PSScriptRoot
$package = 'com.maxbl4.maxdialer'

# --- JDK: use Android Studio's JBR (system Java is often too old for AGP) ---
$jbr = 'C:\Program Files\Android\Android Studio\jbr'
if (Test-Path "$jbr\bin\java.exe") {
    $env:JAVA_HOME = $jbr
} elseif (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set and Android Studio JBR was not found at '$jbr'."
}

# --- Locate adb ---
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path $adb)) { throw "adb not found at '$adb'. Set ANDROID_SDK_ROOT or install platform-tools." }

# --- Resolve target device ---
$devices = @(& $adb devices |
    Select-Object -Skip 1 |
    Where-Object { $_ -match "`tdevice$" } |
    ForEach-Object { ($_ -split "`t")[0] })

if ($devices.Count -eq 0) {
    throw "No authorized devices connected. Check the USB cable and that USB debugging is enabled/authorized."
}
if ($Serial) {
    if ($devices -notcontains $Serial) {
        throw "Device '$Serial' is not connected. Connected: $($devices -join ', ')."
    }
} elseif ($devices.Count -eq 1) {
    $Serial = $devices[0]
} else {
    throw "Multiple devices connected: $($devices -join ', '). Re-run with -Serial <serial>."
}

# --- Build ---
$variant = if ($DebugBuild) { 'debug' } else { 'release' }
Write-Host "Building $variant APK (JAVA_HOME=$env:JAVA_HOME)..." -ForegroundColor Cyan
& "$projectDir\gradlew.bat" ":app:assemble$((Get-Culture).TextInfo.ToTitleCase($variant))" --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)." }

$apk = Join-Path $projectDir "app\build\outputs\apk\$variant\app-$variant.apk"
if (-not (Test-Path $apk)) { throw "APK not found at '$apk'." }
if (-not $DebugBuild) { & "$projectDir\sign-release.ps1" -Apk $apk }

# --- Install ---
Write-Host "Installing to $Serial ..." -ForegroundColor Cyan
& $adb -s $Serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw "adb install failed (exit $LASTEXITCODE)." }

# --- Compile now instead of waiting for the device to do it in the background ---
if (-not $DebugBuild) {
    Write-Host "Compiling ahead of time..." -ForegroundColor Cyan
    & $adb -s $Serial shell cmd package compile -m speed-profile -f $package | Out-Null
}

& $adb -s $Serial shell am start -n "$package/.MainActivity" | Out-Null
Write-Host "Done: installed MaxDialer ($variant) on $Serial." -ForegroundColor Green
