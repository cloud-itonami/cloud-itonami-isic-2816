# Business Model: Manufacture of Lifting and Handling Equipment

## Classification
- Repository: `cloud-itonami-isic-2816`
- ISIC Rev.5: `2816` — manufacture of lifting and handling equipment — unit fabrication, ASME B30/LOLER 1998 proof-load testing and load-test-certificate evidence
- Social impact: industrial-safety, supply-resilience, industrial-jobs

## Customer
- independent crane, hoist and forklift manufacturers needing auditable design-rules and production records
- contract plants producing structures, drums, hooks and drive trains for multiple OEM lifting-equipment brands
- plant/site operators needing verifiable build and load-test history for produced lifting-equipment units
- market regulators (notified bodies under the Machinery Directive/CE marking, OSHA-adjacent authorized inspectors, HSE-adjacent competent persons) needing verifiable design-conformity and proof-load-test evidence
- construction, warehousing and logistics EPC/general contractors that cannot accept closed, unauditable manufacturing-execution platforms

## Offer
- design-rules and jurisdiction-scope version management
- robotics-assisted final assembly, fit-up and proof-load-test rig operation records
- unit proof-load acceptance-test chain-of-custody history
- load-test-certificate drafts and disclosure records
- role-based access and immutable audit ledger
- CSV/EDN audit package export for inspectors

## Revenue
- self-host setup fee
- managed hosting subscription per plant / production line
- support retainer with SLA
- final-assembly/load-test robot integration and maintenance

## Trust Controls
- out-of-spec units are blocked; a load-test certificate is mandatory for release paths; unit history is immutable
- a robot action the governor refuses is never dispatched to hardware
- every dispatch, hold, approval and disclosure path is auditable
- sensitive design and production data stays outside Git
- a fabricated design-rules citation, incomplete evidence, an
  out-of-window unit test load, or an unresolved load-test defect --
  each forces a hold, not an override
- load-test-certificate issuance is logged and escalated, and
  cannot be finalized twice for the same unit
