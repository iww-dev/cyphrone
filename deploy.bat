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
git commit -m "Manual deploy: %date% %time% : Fixed HvH V2 rot. bug and ac detects (Matrix 7) (I tried to fix...) & Added Unilegit CE Rotation!"

REM Принудительный пуш (force push)
echo [*] Force pushing to main branch...
git push -u origin main --force

if %errorlevel% equ 0 (
    echo [+] Deployment successful!
) else (
    echo [!] Deployment failed with error code %errorlevel%
)

pause
