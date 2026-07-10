# cloud-itonami-isic-2816

Open Business Blueprint for **ISIC Rev.5 2816**: manufacture of
lifting and handling equipment -- unit intake, ASME B30/LOLER 1998
proof-load acceptance testing and load-test-certificate issuance for
a community lifting-equipment plant.

This repository publishes a lifting-and-handling-equipment-
manufacturing actor -- unit intake, per-jurisdiction design-rules/
conformity verification, proof-load-test screening, robot
unit-dispatch and load-test-certificate finalization -- as an OSS
business that any qualified crane, hoist or forklift plant can fork,
deploy, run, improve and sell, so a plant keeps its own construction
and load-test history instead of renting a closed MES / quality
SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **Lifting Equipment
Advisor ⊣ Lifting Equipment Governor**.

## Scope note: manufacturing capital equipment, not the site that operates it

This repository is scoped to **building** cranes, hoists, forklifts
and other lifting/handling equipment themselves (overhead/gantry/
tower/mobile cranes, wire-rope and chain hoists, counterbalance
forklifts -- design-rules verification, proof-load testing,
load-test-certificate evidence). It is not a construction/warehouse/
logistics vertical that merely *operates* lifting equipment on site.
Distinct from:

- `cloud-itonami-isic-2410` — basic iron and steel **manufacturing**
- `cloud-itonami-isic-2511` — structural metal products **manufacturing**
- `cloud-itonami-isic-2811` — engines and turbines **manufacturing**
- `cloud-itonami-isic-2813` — pumps, compressors, taps and valves **manufacturing**
- `cloud-itonami-isic-2822` — metal-forming machinery and machine tools **manufacturing**
- `cloud-itonami-isic-2824` — mining/quarrying/construction machinery **manufacturing**
- `cloud-itonami-isic-2910` — motor vehicles **manufacturing**
- `cloud-itonami-isic-3011` — ships and floating structures **manufacturing**

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (final assembly,
fit-up, proof-load-test rig operation) operate under an actor that
proposes actions and an independent **Lifting Equipment Governor**
that gates them. The governor never issues a load-test certificate
itself; `:high`/`:safety-critical` actions (`:actuation/dispatch-
unit`, `:actuation/issue-load-test-certificate`) require human
sign-off.

## Core contract

```text
unit intake + design-rules verify + load-test screen
  -> Lifting Equipment Advisor proposal
  -> Lifting Equipment Governor (HARD holds un-overridable)
  -> phase gate (actuation always escalates)
  -> human approval for high stakes
  -> append-only ledger + draft records
```

## Actuation honesty

Dispatching a final-assembly robot action and issuing a load-test
certificate produce **unsigned draft records and ledger facts only**.
This actor does not talk to real plant control systems or
conformity-marking portals. Signature and hardware dispatch are the
lifting-equipment plant's own acts.

## Ops

| Op | Effect |
|---|---|
| `:unit/intake` | normalize unit directory patch (phase 3 may auto-commit when clean) |
| `:design-rules/verify` | per-jurisdiction design/conformity evidence checklist (always human) |
| `:load-test/screen` | ASME B30/LOLER proof-load-test screen (HARD hold if unresolved) |
| `:actuation/dispatch-unit` | draft unit-dispatch record (always human) |
| `:actuation/issue-load-test-certificate` | draft load-test-certificate record (always human) |

## Social / regulatory hand-off

```clojure
(require '[liftingequip.store :as store]
         '[liftingequip.export :as export])

(def db (store/seed-db))
(export/audit-package db)           ;; EDN maps for conformity/regulator hand-off
(export/package->csv-bundle db)     ;; CSV bundle (units/ledger/dispatches/load-test-certificates)
```

Operator console (static sample): `docs/samples/operator-console.html`.

## Develop

```bash
clojure -M:dev:test
clojure -M:lint
clojure -M:dev:run
```

## License

AGPL-3.0-or-later — see `LICENSE`.

## Operator console (Pages)

After enabling GitHub Pages (Settings → Pages → GitHub Actions), the
static console is at:

https://cloud-itonami.github.io/cloud-itonami-isic-2816/

Local: open `docs/index.html` or `docs/samples/operator-console.html`.

## Export audit package (CLI)

```bash
clojure -M:dev:export
# or: clojure -M:dev:export /tmp/audit-2816
```

Writes CSV files under `out/audit-package/` (or the given directory).
