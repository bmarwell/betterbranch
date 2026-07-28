@echo off
setlocal

set "repo_dir=%~1"
if "%repo_dir%"=="" exit /b 1

if not exist "%repo_dir%" mkdir "%repo_dir%"
cd /d "%repo_dir%"

git init || exit /b 1
git config user.name "BetterBranch IT" || exit /b 1
git config user.email "it@betterbranch.invalid" || exit /b 1

echo base> story.txt
git add story.txt || exit /b 1
git commit -m "initial commit" || exit /b 1
git branch -M main || exit /b 1

git checkout -b feature-one || exit /b 1
echo feature one>> story.txt
git add story.txt || exit /b 1
git commit -m "feature-one commit" || exit /b 1

git checkout main || exit /b 1
echo main update>> story.txt
git add story.txt || exit /b 1
git commit -m "main update" || exit /b 1

git checkout -b feature-two || exit /b 1
echo feature two>> story.txt
git add story.txt || exit /b 1
git commit -m "feature-two commit" || exit /b 1

git checkout main || exit /b 1
