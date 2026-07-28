#!/usr/bin/env sh
set -eu

repo_dir="$1"
mkdir -p "$repo_dir"
cd "$repo_dir"

git init
git config user.name "BetterBranch IT"
git config user.email "it@betterbranch.invalid"
git config commit.gpgsign false

echo "base" > story.txt
git add story.txt
git commit -m "initial commit"
git branch -M main

git checkout -b feature-one
echo "feature one" >> story.txt
git add story.txt
git commit -m "feature-one commit"

git checkout main
echo "main update" >> story.txt
git add story.txt
git commit -m "main update"

git checkout -b feature-two
echo "feature two" >> story.txt
git add story.txt
git commit -m "feature-two commit"

git checkout main
