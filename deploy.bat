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
git commit -m "Manual deploy: %date% %time% : Added 3 new rotations. Sloth Cyphrone Edition (UNstable), HvH V2X Cyphrone Edition (Extended v2x from Aegis) and HvH V2X Cyphrone Edition Rage-Bypass (Its like V2X CE but with more APS (actions persec)   Enjoy this update! (On graph this is 0.4.9-6 of Aegis. )"

REM Принудительный пуш (force push)
echo [*] Force pushing to main branch...
git push -u origin main --force

if %errorlevel% equ 0 (
    echo [+] Deployment successful!
) else (
    echo [!] Deployment failed with error code %errorlevel%
)

pause
