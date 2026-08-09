[CmdletBinding()]
param(
    [string]$AppId = $env:STEAM_APP_ID,
    [string]$DepotId = $env:STEAM_DEPOT_ID,
    [string]$SteamUsername = $env:STEAM_USERNAME,
    [string]$ContentRoot = $env:STEAM_CONTENT_DIR,
    [string]$SteamCmd = $env:STEAMCMD_BIN,
    [string]$Branch = $env:STEAM_BRANCH,
    [string]$Description = $env:STEAM_BUILD_DESCRIPTION
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($ContentRoot)) {
    $ContentRoot = Join-Path $RepoDir "dist\desktop\windows-x86_64\RogueCircuit"
}
if ([string]::IsNullOrWhiteSpace($Description)) {
    $Description = "Rogue Circuit Windows build"
}

$RequiredValues = @{
    AppId = $AppId
    DepotId = $DepotId
    SteamUsername = $SteamUsername
}
foreach ($Required in $RequiredValues.GetEnumerator()) {
    if ([string]::IsNullOrWhiteSpace([string]$Required.Value)) {
        throw "Set $($Required.Key) as a parameter or environment variable."
    }
}
if (-not (Test-Path -LiteralPath $ContentRoot -PathType Container)) {
    throw "Steam content directory does not exist: $ContentRoot"
}

if ([string]::IsNullOrWhiteSpace($SteamCmd)) {
    $Command = Get-Command "steamcmd.exe" -ErrorAction SilentlyContinue
    if ($null -ne $Command) {
        $SteamCmd = $Command.Source
    }
}
if ([string]::IsNullOrWhiteSpace($SteamCmd)) {
    $Candidates = @(
        (Join-Path $env:ProgramFiles "SteamCMD\steamcmd.exe"),
        (Join-Path ${env:ProgramFiles(x86)} "SteamCMD\steamcmd.exe")
    )
    foreach ($Candidate in $Candidates) {
        if (Test-Path -LiteralPath $Candidate) {
            $SteamCmd = $Candidate
            break
        }
    }
}
if ([string]::IsNullOrWhiteSpace($SteamCmd) -or -not (Test-Path -LiteralPath $SteamCmd)) {
    throw "steamcmd.exe was not found. Install SteamCMD or set STEAMCMD_BIN."
}

function Convert-ToVdfPath {
    param([string]$Path)
    return ([IO.Path]::GetFullPath($Path) -replace "\\", "/")
}

function Convert-ToVdfValue {
    param([string]$Value)
    return $Value.Replace('\', '\\').Replace('"', '\"')
}

$BuildDir = Join-Path $RepoDir "target\steam"
$OutputDir = Join-Path $BuildDir "output"
$DepotVdf = Join-Path $BuildDir "depot-$DepotId-windows.vdf"
$AppVdf = Join-Path $BuildDir "app-$AppId-windows.vdf"
New-Item -ItemType Directory -Path $BuildDir, $OutputDir -Force | Out-Null

$VdfContentRoot = Convert-ToVdfPath $ContentRoot
$VdfOutputDir = Convert-ToVdfPath $OutputDir
$VdfDepotFile = Convert-ToVdfPath $DepotVdf
$VdfDescription = Convert-ToVdfValue $Description
$VdfBranch = Convert-ToVdfValue $Branch

@"
"DepotBuildConfig"
{
    "DepotID" "$DepotId"
    "ContentRoot" "$VdfContentRoot"
    "FileMapping"
    {
        "LocalPath" "*"
        "DepotPath" "."
        "recursive" "1"
    }
}
"@ | Set-Content -LiteralPath $DepotVdf -Encoding ASCII

@"
"AppBuild"
{
    "AppID" "$AppId"
    "Desc" "$VdfDescription"
    "BuildOutput" "$VdfOutputDir"
    "ContentRoot" "$VdfContentRoot"
    "SetLive" "$VdfBranch"
    "Depots"
    {
        "$DepotId" "$VdfDepotFile"
    }
}
"@ | Set-Content -LiteralPath $AppVdf -Encoding ASCII

& $SteamCmd +login $SteamUsername +run_app_build $AppVdf +quit
if ($LASTEXITCODE -ne 0) {
    throw "SteamCMD upload failed with exit code $LASTEXITCODE."
}

Write-Host "Steam Windows depot upload completed." -ForegroundColor Green
