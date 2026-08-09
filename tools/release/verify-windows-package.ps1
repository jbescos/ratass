[CmdletBinding()]
param(
    [string]$ContentRoot = "",
    [switch]$LaunchSmokeTest,
    [int]$SmokeTestSeconds = 10
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($ContentRoot)) {
    $ContentRoot = Join-Path $RepoDir "dist\desktop\windows-x86_64\RogueCircuit"
}
$ContentRoot = [IO.Path]::GetFullPath($ContentRoot)

$RequiredFiles = @(
    "RogueCircuit.exe",
    "app\RogueCircuit.cfg",
    "runtime\bin\server\jvm.dll"
)
foreach ($RelativePath in $RequiredFiles) {
    $Path = Join-Path $ContentRoot $RelativePath
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Windows package is missing: $Path"
    }
    if ((Get-Item -LiteralPath $Path).Length -eq 0) {
        throw "Windows package contains an empty file: $Path"
    }
}

$Executable = Join-Path $ContentRoot "RogueCircuit.exe"
$PackageBytes = (
    Get-ChildItem -LiteralPath $ContentRoot -Recurse -File |
        Measure-Object -Property Length -Sum
).Sum
$ExecutableHash = (Get-FileHash -LiteralPath $Executable -Algorithm SHA256).Hash

Write-Host ("Package size: {0:N1} MiB" -f ($PackageBytes / 1MB))
Write-Host "Executable SHA-256: $ExecutableHash"

if ($LaunchSmokeTest) {
    if ($SmokeTestSeconds -lt 3) {
        throw "SmokeTestSeconds must be at least 3."
    }
    $Process = Start-Process -FilePath $Executable -WorkingDirectory $ContentRoot -PassThru
    Start-Sleep -Seconds $SmokeTestSeconds
    if ($Process.HasExited) {
        if ($Process.ExitCode -ne 0) {
            throw "Rogue Circuit exited during the smoke test with code $($Process.ExitCode)."
        }
        Write-Host "Launch smoke test completed: the game exited normally."
    } else {
        Stop-Process -Id $Process.Id
        $Process.WaitForExit()
        Write-Host "Launch smoke test completed: the game remained running for $SmokeTestSeconds seconds."
    }
}

Write-Host "Windows Steam content verified: $ContentRoot" -ForegroundColor Green
