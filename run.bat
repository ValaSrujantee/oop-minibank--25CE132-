
@echo off
cd /d "%~dp0"

if not exist out mkdir out

powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = Get-ChildItem -Path '.\src\main\java' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }; if ($files.Count -eq 0) { Write-Error 'No Java files found in src\main\java'; exit 1 }; & javac -d '.\out' $files; exit $LASTEXITCODE"

if errorlevel 1 (
    echo.
    echo Compilation failed. Check the error above.
    pause
    exit /b 1
)

java -cp out service.MiniBank

pause
