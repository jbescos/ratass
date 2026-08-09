[CmdletBinding()]
param(
    [string]$ProjectVersion = "1.0",
    [string]$OutputRoot = "",
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $RepoDir "dist\desktop"
}

function Find-Executable {
    param([string[]]$Names)
    foreach ($Name in $Names) {
        $Command = Get-Command $Name -ErrorAction SilentlyContinue
        if ($null -ne $Command) {
            return $Command.Source
        }
    }
    return $null
}

if (-not [Environment]::Is64BitOperatingSystem) {
    throw "The Steam Windows depot requires 64-bit Windows."
}

$JPackage = $null
$Jdeps = $null
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $JPackage = Join-Path $JavaHome "bin\jpackage.exe"
    $Jdeps = Join-Path $JavaHome "bin\jdeps.exe"
}
if ([string]::IsNullOrWhiteSpace($JPackage) -or -not (Test-Path -LiteralPath $JPackage)) {
    $JPackage = Find-Executable @("jpackage.exe", "jpackage")
}
if ([string]::IsNullOrWhiteSpace($Jdeps) -or -not (Test-Path -LiteralPath $Jdeps)) {
    $Jdeps = Find-Executable @("jdeps.exe", "jdeps")
}
if ([string]::IsNullOrWhiteSpace($JPackage) -or [string]::IsNullOrWhiteSpace($Jdeps)) {
    throw "jpackage and jdeps were not found. Install a full JDK 21 and set JAVA_HOME."
}

$Maven = Find-Executable @("mvn.cmd", "mvn.exe", "mvn")
if (-not $SkipBuild -and [string]::IsNullOrWhiteSpace($Maven)) {
    throw "Maven was not found. Install Maven or use -SkipBuild with an existing desktop jar."
}

if (-not $SkipBuild) {
    & $Maven -f (Join-Path $RepoDir "pom.xml") -pl desktop -am clean package "-DskipTests"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven desktop build failed with exit code $LASTEXITCODE."
    }
}

$JarPath = Join-Path $RepoDir "desktop\target\ratass-desktop-$ProjectVersion.jar"
$IconPath = Join-Path $RepoDir "assets\branding\rogue-circuit.ico"
if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "Desktop application jar not found: $JarPath"
}
if (-not (Test-Path -LiteralPath $IconPath)) {
    throw "Windows package icon not found: $IconPath"
}

$PackageDir = Join-Path $OutputRoot "windows-x86_64"
$PackageRoot = Join-Path $PackageDir "RogueCircuit"
$InputDir = Join-Path $RepoDir "desktop\target\jpackage-input-windows"
if (Test-Path -LiteralPath $PackageDir) {
    Remove-Item -LiteralPath $PackageDir -Recurse -Force
}
if (Test-Path -LiteralPath $InputDir) {
    Remove-Item -LiteralPath $InputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $PackageDir, $InputDir -Force | Out-Null
Copy-Item -LiteralPath $JarPath -Destination $InputDir

$ModuleOutput = @(
    & $Jdeps `
        --ignore-missing-deps `
        --multi-release base `
        --print-module-deps `
        $JarPath
)
if ($LASTEXITCODE -ne 0) {
    throw "jdeps failed with exit code $LASTEXITCODE."
}
$RuntimeModules = ($ModuleOutput -join "").Trim()
if ([string]::IsNullOrWhiteSpace($RuntimeModules)) {
    throw "Could not determine the Java modules required by the desktop game."
}

$JPackageArgs = @(
    "--type", "app-image",
    "--name", "RogueCircuit",
    "--description", "Roguelite circuit racing",
    "--vendor", "jbescos",
    "--app-version", $ProjectVersion,
    "--input", $InputDir,
    "--main-jar", (Split-Path $JarPath -Leaf),
    "--main-class", "com.github.jbescos.DesktopLauncher",
    "--dest", $PackageDir,
    "--add-modules", $RuntimeModules,
    "--java-options", "-Dfile.encoding=UTF-8",
    "--icon", $IconPath
)

& $JPackage @JPackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE."
}

$Executable = Join-Path $PackageRoot "RogueCircuit.exe"
if (-not (Test-Path -LiteralPath $Executable)) {
    throw "Windows package did not contain the expected executable: $Executable"
}

Write-Host "Packaged Rogue Circuit for Windows: $PackageRoot" -ForegroundColor Green
