# Fix CI: outdated single-variant build commands

The error happens when CI still uses `:sdk:assembleRelease` / `:applovin-adapter:assembleRelease` or references `sdk-release.aar` / `applovin-adapter-release.aar`. Since **1.2.6**, the project publishes **four** flavor variants per module.

## Correct build command

```yaml
      - name: Build all AAR variants
        env:
          BidscubeVersion: ${{ steps.version.outputs.version }}
          BidscubeAdapterVersion: ${{ steps.version.outputs.version }}
        run: ./gradlew clean stageAllReleaseAars -PskipSigning=true --no-daemon --stacktrace
```

Expected output: **8** files under `build/staged-aars/`.

## Workflows in this repo

- **`publish.yml`** — tag `v*` (full SDK + adapter release)
- **`release-applovin-adapter.yml`** — tag `applovin-adapter-v*`

Both workflows are updated for multi-variant AAR export. Copy them from this project if your GitHub repo still has the old single-AAR workflow.

See [RELEASE.md](../RELEASE.md) for the maintainer checklist.
