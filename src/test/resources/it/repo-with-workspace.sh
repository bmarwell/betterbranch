#!/usr/bin/env sh
set -eu

repo_dir="$1"
mkdir -p "$repo_dir"
cd "$repo_dir"

mkdir bare
cd bare

git init --bare
git config user.name "BetterBranch IT"
git config user.email "it@betterbranch.invalid"
git config commit.gpgsign false

git worktree add ../main
cd ../main
echo "base" >story.txt
git add story.txt
git commit -m "initial commit"
git branch -M main

cd "$repo_dir/bare"
git worktree add -b branch1 ../branch1 main
cd ../branch1
echo "branch1 update" >>story.txt
git add story.txt
git commit -m "branch1 update"
