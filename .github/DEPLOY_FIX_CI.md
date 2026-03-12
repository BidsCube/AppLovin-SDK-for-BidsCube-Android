# Fix CI: "sourcesJar not found" in AppLovin-SDK-for-BidsCube-Android

The error happens because **the workflow file on GitHub** (repo `AppLovin-SDK-for-BidsCube-Android`) still uses the old command with `:sdk:sourcesJar` and `:sdk:javadocJar`, which do not exist in this project.

## Fix on GitHub (choose one)

### Option A: Replace the workflow file via Git (recommended)

1. Open the repo **AppLovin-SDK-for-BidsCube-Android** (the one where Actions run).
2. Ensure this folder (`bidscube-sdk-android`) is that repo or is in sync with it:
   - If the same repo: commit and push the file `.github/workflows/publish.yml` from this project to the default branch (e.g. `main`).
   - If a different clone: copy `bidscube-sdk-android/.github/workflows/publish.yml` into `AppLovin-SDK-for-BidsCube-Android/.github/workflows/publish.yml`, then commit and push from the AppLovin-SDK-for-BidsCube-Android clone.
3. Re-run the workflow (push a new tag like `v1.0.0` or use "Run workflow" on the Actions tab). The workflow that runs will be the one from the **branch you pushed** (or the commit the tag points to).

### Option B: Edit directly on GitHub

1. Go to **https://github.com/YOUR_ORG/AppLovin-SDK-for-BidsCube-Android**
2. Open **.github/workflows/publish.yml**
3. Click **Edit** (pencil icon).
4. Find the step that runs the build (it contains `if [ -f "./gradlew" ]; then` and `sourcesJar`).
5. Replace the whole **Build** step with exactly this (one step, one run line):

```yaml
      - name: Build AAR
        run: ./gradlew clean :sdk:assembleRelease --no-daemon --stacktrace
```

6. Remove any other line in that step that mentions `sourcesJar` or `javadocJar`.
7. Ensure the steps **before** this create `gradlew` when it is missing (steps "Ensure Gradle is available" and "Create Gradle wrapper (when missing)"). If they are missing, replace the entire workflow file with the content of **publish.yml** from this project.
8. Commit the change to the default branch.

### Option C: Use this project’s workflow as single source of truth

- Copy the **entire** content of **.github/workflows/publish.yml** from this project (bidscube-sdk-android) into the file **.github/workflows/publish.yml** in the repo **AppLovin-SDK-for-BidsCube-Android** on GitHub, then commit.

## Important

- CI runs the workflow from the **commit** that is checked out (e.g. the tag’s commit). So the updated `publish.yml` must be in that commit. Push the change first, then create or move the tag to that commit and push the tag.
- This project’s **.github/workflows/publish.yml** already contains the correct command (only `:sdk:assembleRelease`). The runner log you see is from the copy of the workflow that lives in the GitHub repo; that copy must be updated.
