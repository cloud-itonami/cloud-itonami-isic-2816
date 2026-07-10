# ADR-0001: Lifting Equipment Advisor ⊣ Lifting Equipment Governor architecture

- Status: Accepted (2026-07-10)
- Repository: `cloud-itonami-isic-2816` (ISIC Rev.5 `2816`)

## Context

Lifting-and-handling-equipment manufacturing (final assembly, ASME
B30 series / OSHA 1926.1400 / LOLER 1998 proof-load acceptance
testing, design-rules/conformity marking, load-test-certificate
issuance) needs the same governed-actor pattern as the rest of the
cloud-itonami fleet: an untrusted advisor proposes; an independent
governor may HOLD; high-stakes actuation never auto-commits.

This vertical continues the classic heavy-industry manufacturing
cluster after `cloud-itonami-isic-2410` (basic iron and steel),
`cloud-itonami-isic-2811` (engines and turbines), `cloud-itonami-
isic-2910` (motor vehicles), `cloud-itonami-isic-3011` (ships and
floating structures), `cloud-itonami-isic-2511` (structural metal
products), `cloud-itonami-isic-2822` (metal-forming machinery and
machine tools), `cloud-itonami-isic-2824` (mining/quarrying/
construction machinery) and `cloud-itonami-isic-2813` (pumps,
compressors, taps and valves) -- and is another entry in the fleet's
capital-equipment/general-purpose-machinery sub-cluster alongside
2822/2824/2813 (the machines/equipment that make or serve other
machines), distinct from the transport-equipment sub-cluster
(2811/2910/3011).

## Decision

1. Namespaces live under `liftingequip.*` with the standard
   facts / registry / store / governor / phase / advisor / operation / sim
   shape.
2. Entity is a lifting-equipment **unit** (a crane, hoist or
   forklift), not a vehicle, hull block, steel heat, machine tool or
   pressure-equipment unit.
3. Dual actuation on the same entity:
   - `:actuation/dispatch-unit` (robot final-assembly/shipment dispatch draft)
   - `:actuation/issue-load-test-certificate` (ASME B30/LOLER proof-load-test-certificate draft)
4. Double-actuation guards use dedicated booleans
   (`:unit-dispatched?`, `:load-test-certified?`), never a status
   lifecycle (ADR-2607071320 / 6492 lesson).
5. `unit-test-load-out-of-range?` continues the fleet's two-sided
   range check family (after testlab / conservation / water /
   steelworks / turbine / automotive / machinetool / heavyequip /
   pressureequip), applied here to a unit's own measured proof-load
   test result against its own recorded rated-load spec bounds --
   the direct analog of ASME B30's required proof-load test (typically
   100-125% of rated capacity / Safe Working Load, SWL) and the
   code's overstress ceiling (a test load high enough to risk
   permanent deformation is out of range on the other side).
6. Load-test defect unresolved is evaluated unconditionally so
   `:load-test/screen` itself can HARD-hold (parksafety
   ADR-2607071922 Decision 5 discipline).
7. Spec-basis catalog seeds JPN (MHLW/METI/JIS B 8821·B 8830, 労働安
   全衛生法クレーン等安全規則) / USA (ASME B30 series + OSHA 29 CFR
   1926.1400) / GBR (LOLER 1998 + BS EN 13001) / DEU (Machinery
   Directive 2006/42/EC + DIN EN 13001) only. Missing jurisdictions
   are uncovered, never fabricated.

## Consequences

(+) Lifting-and-handling-equipment manufacturing gains a forkable OSS
operating stack with auditable governor holds.
(+) Reuses langgraph + store dual-backend parity without new physics.
(−) No physical plant digital-twin tick in this repo (follow-up
domain data is out of scope here).
(−) Design-conformity-authority coverage is a starting catalog, not
exhaustive.

## Related

- Superproject fleet ADR for this promotion (lifting-equipment-2816-coverage)
- Sibling architecture: `cloud-itonami-isic-2410` docs/adr/0001,
  `cloud-itonami-isic-2811` docs/adr/0001, `cloud-itonami-isic-2910`
  docs/adr/0001, `cloud-itonami-isic-3011` docs/adr/0001,
  `cloud-itonami-isic-2511` docs/adr/0001, `cloud-itonami-isic-2822`
  docs/adr/0001, `cloud-itonami-isic-2824` docs/adr/0001,
  `cloud-itonami-isic-2813` docs/adr/0001
