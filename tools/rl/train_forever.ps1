Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir "..\..")

function Get-EnvValue {
    param(
        [string] $Name,
        [string] $DefaultValue
    )

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }
    return $value
}

function Resolve-RepoPath {
    param([string] $PathValue)

    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return $PathValue
    }

    return [System.IO.Path]::GetFullPath((Join-Path $RepoRoot $PathValue))
}

function Invoke-Checked {
    param(
        [string] $Executable,
        [string[]] $Arguments
    )

    if (($Executable.Contains("\") -or $Executable.Contains("/")) -and -not (Test-Path $Executable)) {
        throw "Executable not found: $Executable. Create the virtual environment with: py -3 -m venv .venv-rl; .venv-rl\Scripts\python.exe -m pip install -r tools\rl\requirements.txt"
    }

    Write-Host ("running={0} {1}" -f $Executable, ($Arguments -join " "))
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Executable failed with exit code $LASTEXITCODE"
    }
}

function Use-ConfiguredJava {
    $javaHome = Get-EnvValue "RL_JAVA_HOME" ""
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        return
    }

    $resolvedJavaHome = [string](Resolve-Path $javaHome)
    $javaExe = Join-Path $resolvedJavaHome "bin\java.exe"
    if (-not (Test-Path $javaExe)) {
        throw "RL_JAVA_HOME does not contain bin\java.exe: $resolvedJavaHome"
    }

    $env:JAVA_HOME = [string] $resolvedJavaHome
    $env:Path = (Join-Path $env:JAVA_HOME "bin") + [System.IO.Path]::PathSeparator + $env:Path
    Write-Host "java_home=$env:JAVA_HOME"
}

function Get-JavaMajorVersion {
    param([string[]] $VersionOutput)

    $text = $VersionOutput -join " "
    if ($text -match 'version "([0-9]+)(?:\.([0-9]+))?') {
        $major = [int] $Matches[1]
        if ($major -eq 1 -and $Matches[2]) {
            return [int] $Matches[2]
        }
        return $major
    }

    throw "Could not parse Java version from: $text"
}

function Assert-SupportedJava {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw "java.exe was not found. Install JDK 17 or 21, or set RL_JAVA_HOME to an installed JDK."
    }

    $versionOutput = @(java -version 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "java -version failed. Install JDK 17 or 21 and make sure java.exe is on PATH."
    }

    $versionText = (($versionOutput -join " ") -replace '\s+', ' ').Trim()
    $major = Get-JavaMajorVersion $versionOutput
    Write-Host "java_version=$versionText"

    if ($major -gt 21 -and (Get-EnvValue "RL_ALLOW_UNTESTED_JAVA" "0") -ne "1") {
        throw "Java $major was detected. Windows RL training embeds Java in the Python/Ray process through JPype, and Java 25 has crashed in _raylet.pyd. Install JDK 17 or 21 and set RL_JAVA_HOME to it, or set RL_ALLOW_UNTESTED_JAVA=1 to override."
    }
}

function Ensure-PythonEnvironment {
    if (Test-Path $PythonBin) {
        return
    }

    $configuredPython = [Environment]::GetEnvironmentVariable("PYTHON_BIN")
    if (-not [string]::IsNullOrWhiteSpace($configuredPython)) {
        throw "PYTHON_BIN points to a missing executable: $PythonBin"
    }

    $autoSetup = Get-EnvValue "RL_AUTO_SETUP_VENV" "1"
    if ($autoSetup -eq "0") {
        throw "Executable not found: $PythonBin. Create the virtual environment with: py -3 -m venv .venv-rl; .venv-rl\Scripts\python.exe -m pip install -r tools\rl\requirements.txt"
    }

    Write-Host "creating_virtualenv=.venv-rl"
    Invoke-Checked "py" @("-3", "-m", "venv", ".venv-rl")
    Invoke-Checked $PythonBin @("-m", "pip", "install", "-r", "tools\rl\requirements.txt")
}

$PythonBin = Get-EnvValue "PYTHON_BIN" ".venv-rl\Scripts\python.exe"
$Workers = Get-EnvValue "RL_WORKERS" "0"
$ControlledAgents = Get-EnvValue "RL_CONTROLLED_AGENTS" "6"
$FieldSize = Get-EnvValue "RL_FIELD_SIZE" "12"
$ActionRepeat = Get-EnvValue "RL_ACTION_REPEAT" "4"
$MaxActionSteps = Get-EnvValue "RL_MAX_ACTION_STEPS" "900"
$TrainBatchSize = Get-EnvValue "RL_TRAIN_BATCH_SIZE" "4096"
$MinibatchSize = Get-EnvValue "RL_MINIBATCH_SIZE" "512"
$CheckpointEvery = Get-EnvValue "RL_CHECKPOINT_EVERY" "20"
$CheckpointDir = Get-EnvValue "RL_CHECKPOINT_DIR" "rl-checkpoints-direct-circle"
$IterationsPerCycle = [int](Get-EnvValue "RL_FOREVER_ITERATIONS" "100")
$PackageEveryCycles = [int](Get-EnvValue "RL_PACKAGE_EVERY_CYCLES" "1")
$NumGpus = Get-EnvValue "RL_NUM_GPUS" "0"
$MapIds = Get-EnvValue "RL_MAP_IDS" ""
$RayNumCpus = Get-EnvValue "RL_RAY_NUM_CPUS" "0"
$RayTempDir = Get-EnvValue "RL_RAY_TEMP_DIR" ""
$BuildBeforeTraining = Get-EnvValue "RL_BUILD_BEFORE_TRAINING" "1"
$DesktopJar = Get-EnvValue "RL_JAR" "desktop\target\ratass-desktop-1.0.jar"
$PythonBin = Resolve-RepoPath $PythonBin
$DesktopJar = Resolve-RepoPath $DesktopJar

$CheckpointFile = Join-Path $CheckpointDir "rllib_checkpoint.json"
$CommonArgs = @(
    "--checkpoint-dir", $CheckpointDir,
    "--workers", $Workers,
    "--controlled-agents", $ControlledAgents,
    "--field-size", $FieldSize,
    "--action-repeat", $ActionRepeat,
    "--max-action-steps", $MaxActionSteps,
    "--train-batch-size", $TrainBatchSize,
    "--minibatch-size", $MinibatchSize,
    "--checkpoint-every", $CheckpointEvery,
    "--num-gpus", $NumGpus
)

if (-not [string]::IsNullOrWhiteSpace($MapIds)) {
    $CommonArgs += @("--map-ids", $MapIds)
}
if ($RayNumCpus -ne "0") {
    $CommonArgs += @("--ray-num-cpus", $RayNumCpus)
}
if (-not [string]::IsNullOrWhiteSpace($RayTempDir)) {
    $CommonArgs += @("--ray-temp-dir", $RayTempDir)
}

function Export-Policy {
    if (-not (Test-Path $CheckpointFile)) {
        Write-Host "checkpoint_missing=$CheckpointFile"
        return
    }

    Invoke-Checked $PythonBin @("tools\rl\export_policy.py", "--checkpoint-dir", $CheckpointDir)
}

function Package-Game {
    Invoke-Checked "mvn" @("-pl", "desktop", "-am", "package")
}

function Should-PackageCycle {
    param([int] $Cycle)
    return $PackageEveryCycles -ne 0 -and ($Cycle % $PackageEveryCycles) -eq 0
}

function Finish-FromLatestCheckpoint {
    Export-Policy
    if (Should-PackageCycle 1) {
        Package-Game
    }
}

$Cycle = 0
$TrainingInProgress = $false

Push-Location $RepoRoot
try {
    Use-ConfiguredJava
    Assert-SupportedJava
    Ensure-PythonEnvironment

    if ($BuildBeforeTraining -eq "1" -or -not (Test-Path $DesktopJar)) {
        Package-Game
    }

    while ($true) {
        $Cycle += 1
        $Resume = $false
        $CycleArgs = @($CommonArgs)
        if (Test-Path $CheckpointFile) {
            $Resume = $true
            $CycleArgs += "--resume"
        }

        Write-Host "cycle=$Cycle iterations=$IterationsPerCycle resume=$Resume"
        $TrainingInProgress = $true
        Invoke-Checked $PythonBin (@("tools\rl\train_rllib.py") + $CycleArgs + @("--iterations", "$IterationsPerCycle"))
        $TrainingInProgress = $false

        Export-Policy
        if (Should-PackageCycle $Cycle) {
            Package-Game
        }
    }
}
finally {
    if ($TrainingInProgress) {
        Write-Host "interrupted=1"
        try {
            Finish-FromLatestCheckpoint
        } catch {
            Write-Warning $_
        }
    }
    Pop-Location
}
