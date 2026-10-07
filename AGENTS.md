# AGENTS.md

KelpLib — a Minecraft Paper/Folia + Velocity plugin library (shared utility & extension API for the utoverse server family). Implemented per [PLAN.md](PLAN.md) v0.7 (M0–M6). Current version: `1.0.0` (see `gradle.properties` — **version lives there, never hardcode it**).

## Build & Run

- Gradle wrapper is present (`gradlew` / `gradlew.bat`, Gradle 9.4.0; use the wrapper, not a system install).
  - Note: system Gradle on this machine is 9.4.1 — plugin versions in the build are pinned to what 9.4.x accepts (shadow `9.5.1`, run-paper `3.0.2`; newer lines need gradle-plugin-api 9.7.0).
- `gradlew build` — compiles all modules and produces:
  - `kelpLib-bukkit/build/libs/kelpLib-bukkit-<v>-all.jar` (Bukkit/Folia plugin, shadow jar)
  - `kelpLib-velocity/build/libs/kelpLib-velocity-<v>-all.jar` (Velocity proxy plugin, shadow jar with relocated internals under `ink.tuanzi.kelpLib.libs.*`)
  - `kelpLib-api` / `kelpLib-core` plain jars.
- `gradlew :kelpLib-api:publishToMavenLocal` — publishes the api artifact (`ink.tuanzi:kelpLib-api`) for business plugins (0.x distribution channel, §8).
- `gradlew :kelpLib-bukkit:runServer` — local Paper 26.3 smoke run (run-paper, `run/` is gitignored).
- `.github/workflows/release.yml` — on `v*` tags / manual dispatch: publishes api/core/bukkit/velocity to GitHub Packages (bukkit/velocity include the shadow jar under the `all` classifier) and deploys the api JavaDoc to GitHub Pages. The GitHub Packages repo is only registered in the build when `GITHUB_TOKEN` is present, so local `publishToMavenLocal` is unaffected.
- Configuration cache, parallel, and build cache are enabled; prefer plain task invocations.

## Architecture (multi-module, §4 of PLAN.md)

```
kelpLib-api/       // platform-agnostic stable surface (interfaces/annotations/models only)
                   //   Kelp facade, scheduler/, promise/, terminable/, command/(annotations),
                   //   config/, storage/, messenger/, text/, util/  — NO platform deps (P1)
kelpLib-core/      // platform-agnostic implementations (Promise/Terminable engine, config engine,
                   //   storage file/sql/redis, Messenger + Redis transport, Messages/Display,
                   //   annotation command scanner)
kelpLib-bukkit/    // Paper/Folia platform module: schedulers (Paper + Folia, detected via
                   //   RegionizedServer), RegionSchedulerView, functional Events, ItemBuilder,
                   //   Nbt (optional/reflection), KelpGui gate, self-built command layer,
                   //   ScoreboardDisplay, plugin-messaging transport, /kelp demos, KelpLib entry
kelpLib-velocity/  // Velocity platform module: scheduler, command layer, Display subset,
                   //   plugin-messaging transport, /kelp demos, KelpLibVelocity entry
```

Dependency rule (build-enforced by convention): `kelpLib-api`/`kelpLib-core` declare **no** platform dependencies; platform interaction only exists in the platform modules.

## ⚠️ Compile-time stubs (`src/stubs/java` in both platform modules)

During development, international Maven repos were unreachable from this machine, so `paper-api` / `velocity-api` are mirrored by **local stub source sets** that mirror only the API subset used. They are compile-only — **never packaged into jars** (verified). To switch back to real dependencies (when repos are reachable): uncomment the `paper-api` / `velocity-api` lines in the two platform `build.gradle.kts` files and delete the `sourceSets.create("stubs")` + `compileOnly(sourceSets["stubs"].output)` lines. Stubs deliberately keep long-stable (1.21-era) signatures; runtime uses the server's real classes.

Same situation drove these PLAN-sanctioned fallbacks (see README "环境受限偏差记录"):
- LiteCommands/Cloud unavailable → self-built annotation command layer (`@Command/@Execute/@Arg/@Sender/@Flag`, scanner in `kelpLib-core/command`).
- InvUI unavailable → `KelpGui` capability-gated scaffold (`available()==false`), `Schemes` sugar in place.
- ItemNBTAPI unavailable → reflective optional integration (`Nbt.available()`), degrades to PDC.

## Conventions & Gotchas

- Java 21 toolchain, `options.release = 21` everywhere (1.21.x servers run Java 21; no post-21 language features, P5).
- `plugin.yml` lives in `kelpLib-bukkit/src/main/resources` with `version: '${version}'` expanded from the Gradle project version; `velocity-plugin.json` in `kelpLib-velocity/src/main/resources` likewise. Change versions in `gradle.properties` only.
- Velocity plugin id is `kelplib` (`@Plugin` annotation + `velocity-plugin.json`); keep both in sync.
- `plugin.yml` declares `api-version: '1.21'`, `folia-supported: true`, `load: STARTUP` — KelpLib initializes its facade in `onLoad()` so STARTUP dependents can consume it there.
- Internal-type runtime deps (HikariCP, Lettuce, JDBC drivers) are **not** bundled in the Bukkit jar — they come via `plugin.yml` `libraries:`; on Velocity they are shadow-relocated into the plugin jar. Don't add them as `implementation` in the bukkit module.
- Every cancellable resource implements `Terminable` and gets bound to the owning plugin's `TerminableConsumer` (`KelpBukkit.lifecycle(plugin)`); plugin disable must leave zero residue (P9).
- All thread operations go through `KelpScheduler` (P2) — never call `Bukkit.getScheduler()` directly in library code.
- Keep Gradle `group` (`ink.tuanzi`) and package naming (`ink.tuanzi.kelpLib.*`) consistent when adding new packages.
