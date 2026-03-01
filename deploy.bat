@echo off
setlocal enabledelayedexpansion

REM Автодеплой на GitHub без clone
REM Принудительный пуш всех файлов

echo [*] Starting deployment to GitHub...

REM Проверяем есть ли .git
if not exist .git (
    echo [!] Initializing git repository...
    git init
    git remote add origin https://github.com/iww-dev/cyphrone.git
) else (
    echo [+] Git repository already exists
)

REM Добавляем все файлы
echo [*] Adding all files...
git add -A

REM Коммитим
echo [*] Creating commit...
git commit -m "Fix camera jitter and improve GUI visuals - V2X jitterStrength reduced, velocity prediction added, inside-enemy detection, HWAngle variable conflicts fixed, MainMenu button labels added, MenuScreen resize sync improved, PressableWidgetRender text overflow fixed, GUI transparency enhanced, StrikeManager logic optimized"

REM Принудительный пуш (force push)
echo [*] Force pushing to main branch...
git push -u origin main --force

if %errorlevel% equ 0 (
    echo [+] Deployment successful!
) else (
    echo [!] Deployment failed with error code %errorlevel%
)

pause
