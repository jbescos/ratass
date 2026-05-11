@echo off
setlocal

if "%RL_OBJECTIVE%"=="" set "RL_OBJECTIVE=navigation"
if "%RL_CHECKPOINT_DIR%"=="" set "RL_CHECKPOINT_DIR=rl-checkpoints-navigation-route"
if "%RL_CONTROLLED_AGENTS%"=="" set "RL_CONTROLLED_AGENTS=1"
if "%RL_FIELD_SIZE%"=="" set "RL_FIELD_SIZE=1"
if "%RL_MAX_ACTION_STEPS%"=="" set "RL_MAX_ACTION_STEPS=1200"

set "SCRIPT_DIR=%~dp0"
call "%SCRIPT_DIR%train_forever.cmd" %*
exit /b %ERRORLEVEL%
