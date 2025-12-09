@echo off
setlocal enabledelayedexpansion

set SID=%1
set PID=%2

set BASE=D:\text\javaHW\javaCourse_oj
set SUB=%BASE%\data\submission\%SID%
set BUILD=%SUB%\build
set PROBLEM=%BASE%\data\problem\%PID%\testCases

echo [run_submission.bat] SID=%SID%, PID=%PID%
echo [run_submission.bat] BUILD=%BUILD%
echo [run_submission.bat] PROBLEM=%PROBLEM%
echo [run_submission.bat] SUB=%SUB%

echo [run_submission.bat] running docker...
docker run --rm ^
  --name run-sub-%SID%-%PID% ^
  --entrypoint /app/run-submission.sh ^
  --memory 256m ^
  -v "%BUILD%":/app/build:ro ^
  -v "%PROBLEM%":/app/input:ro ^
  -v "%SUB%":/app/submission ^
  cpp-run ^
  /app/build/main /app/submission/cases.yaml /app/submission/results.yaml

echo [run_submission.bat] docker exit code: %ERRORLEVEL%

endlocal
exit /b %ERRORLEVEL%