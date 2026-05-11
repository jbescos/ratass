@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..") do set "REPO_ROOT=%%~fI"

pushd "%REPO_ROOT%"
if errorlevel 1 exit /b 1

if "%RL_DOCKER_IMAGE%"=="" set "RL_DOCKER_IMAGE=ratass-rl:latest"
if "%RL_OBJECTIVE%"=="" set "RL_OBJECTIVE=combat"
if "%RL_DOCKER_SHM_SIZE%"=="" set "RL_DOCKER_SHM_SIZE=4g"

if "%RL_OBJECTIVE%"=="navigation" (
    set "DEFAULT_CONTROLLED_AGENTS=1"
    set "DEFAULT_FIELD_SIZE=1"
    set "DEFAULT_MAX_ACTION_STEPS=1200"
    set "DEFAULT_CHECKPOINT_DIR=rl-checkpoints-navigation-route"
) else (
    set "DEFAULT_CONTROLLED_AGENTS=6"
    set "DEFAULT_FIELD_SIZE=12"
    set "DEFAULT_MAX_ACTION_STEPS=900"
    set "DEFAULT_CHECKPOINT_DIR=rl-checkpoints-direct-circle-route"
)

if "%RL_CHECKPOINT_DIR%"=="" set "RL_CHECKPOINT_DIR=%DEFAULT_CHECKPOINT_DIR%"
if "%RL_CONTROLLED_AGENTS%"=="" set "RL_CONTROLLED_AGENTS=%DEFAULT_CONTROLLED_AGENTS%"
if "%RL_FIELD_SIZE%"=="" set "RL_FIELD_SIZE=%DEFAULT_FIELD_SIZE%"
if "%RL_MAX_ACTION_STEPS%"=="" set "RL_MAX_ACTION_STEPS=%DEFAULT_MAX_ACTION_STEPS%"
if "%RL_FOREVER_ITERATIONS%"=="" set "RL_FOREVER_ITERATIONS=100"
if "%RL_MAX_CYCLES%"=="" set "RL_MAX_CYCLES=0"
if "%RL_CHECKPOINT_EVERY%"=="" set "RL_CHECKPOINT_EVERY=20"
if "%RL_WORKERS%"=="" set "RL_WORKERS=0"
if "%RL_NUM_GPUS%"=="" set "RL_NUM_GPUS=0"
if "%RL_NO_REWARD_SUMMARY%"=="" set "RL_NO_REWARD_SUMMARY=0"
if "%RL_PACKAGE_EVERY_CYCLES%"=="" set "RL_PACKAGE_EVERY_CYCLES=1"
if "%RL_BUILD_BEFORE_TRAINING%"=="" set "RL_BUILD_BEFORE_TRAINING=1"
if "%RL_RAY_TEMP_DIR%"=="" set "RL_RAY_TEMP_DIR=rl-logs/ray"

if not exist ".docker-home\.m2" mkdir ".docker-home\.m2"
if not exist "rl-logs" mkdir "rl-logs"

if not "%RL_DOCKER_BUILD%"=="0" (
    docker build -f tools\rl\Dockerfile -t "%RL_DOCKER_IMAGE%" .
    if errorlevel 1 goto :fail
)

set "GPU_ARGS="
if "%RL_DOCKER_GPU%"=="1" set "GPU_ARGS=--gpus all"

docker run --rm -it --shm-size=%RL_DOCKER_SHM_SIZE% %GPU_ARGS% ^
  -v "%CD%:/workspace/ratass" ^
  -v "%CD%\.docker-home:/home/ratass" ^
  -e HOME=/home/ratass ^
  -e MAVEN_CONFIG=/home/ratass/.m2 ^
  -e "RL_OBJECTIVE=%RL_OBJECTIVE%" ^
  -e "RL_CHECKPOINT_DIR=%RL_CHECKPOINT_DIR%" ^
  -e "RL_CONTROLLED_AGENTS=%RL_CONTROLLED_AGENTS%" ^
  -e "RL_FIELD_SIZE=%RL_FIELD_SIZE%" ^
  -e "RL_MAX_ACTION_STEPS=%RL_MAX_ACTION_STEPS%" ^
  -e "RL_FOREVER_ITERATIONS=%RL_FOREVER_ITERATIONS%" ^
  -e "RL_MAX_CYCLES=%RL_MAX_CYCLES%" ^
  -e "RL_CHECKPOINT_EVERY=%RL_CHECKPOINT_EVERY%" ^
  -e "RL_WORKERS=%RL_WORKERS%" ^
  -e "RL_NUM_GPUS=%RL_NUM_GPUS%" ^
  -e "RL_NO_REWARD_SUMMARY=%RL_NO_REWARD_SUMMARY%" ^
  -e "RL_PACKAGE_EVERY_CYCLES=%RL_PACKAGE_EVERY_CYCLES%" ^
  -e "RL_BUILD_BEFORE_TRAINING=%RL_BUILD_BEFORE_TRAINING%" ^
  -e "RL_RAY_TEMP_DIR=%RL_RAY_TEMP_DIR%" ^
  -e "RL_MAP_IDS=%RL_MAP_IDS%" ^
  -e "RL_OPPONENT_COUNT=%RL_OPPONENT_COUNT%" ^
  %RL_DOCKER_EXTRA_ARGS% ^
  "%RL_DOCKER_IMAGE%" bash tools/rl/docker_train_entrypoint.sh %*

set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%

:fail
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%
