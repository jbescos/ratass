[CmdletBinding()]
param(
    [string]$ProjectVersion = "1.0",
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SkipBuild,
    [switch]$LaunchSmokeTest,
    [switch]$Upload,
    [string]$AppId = $env:STEAM_APP_ID,
    [string]$DepotId = $env:STEAM_DEPOT_ID,
    [string]$SteamUsername = $env:STEAM_USERNAME,
    [string]$SteamCmd = $env:STEAMCMD_BIN,
    [string]$Branch = $env:STEAM_BRANCH,
    [string]$Description = $env:STEAM_BUILD_DESCRIPTION
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$PackageArguments = @{
    ProjectVersion = $ProjectVersion
    JavaHome = $JavaHome
}
if ($SkipBuild) {
    $PackageArguments.SkipBuild = $true
}
& (Join-Path $PSScriptRoot "package-windows.ps1") @PackageArguments

$RepoDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$ContentRoot = Join-Path $RepoDir "dist\desktop\windows-x86_64\RogueCircuit"
$VerifyArguments = @{ ContentRoot = $ContentRoot }
if ($LaunchSmokeTest) {
    $VerifyArguments.LaunchSmokeTest = $true
}
& (Join-Path $PSScriptRoot "verify-windows-package.ps1") @VerifyArguments

if ($Upload) {
    & (Join-Path $PSScriptRoot "publish-steam-windows.ps1") `
        -AppId $AppId `
        -DepotId $DepotId `
        -SteamUsername $SteamUsername `
        -SteamCmd $SteamCmd `
        -Branch $Branch `
        -Description $Description `
        -ContentRoot $ContentRoot
} else {
    Write-Host "Package verified. Re-run with -Upload when the Steam IDs are configured."
}
