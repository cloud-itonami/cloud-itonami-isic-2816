(ns liftingequip.registry
  "Pure-function unit-dispatch + load-test-certificate record
  construction -- an append-only lifting-equipment-manufacturer
  book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a unit-dispatch or
  load-test-certificate reference number -- every manufacturer/
  jurisdiction assigns its own reference format. This namespace does
  NOT invent one; it builds a jurisdiction-scoped sequence number and
  validates the record's required fields, the same honest, non-
  fabricating discipline `liftingequip.facts` uses.

  `unit-test-load-out-of-range?` continues this fleet's two-sided
  range check family (`testlab.registry/within-tolerance?` /
  `conservation.registry/body-condition-out-of-range?` /
  `water.registry/contaminant-level-out-of-range?` /
  `steelworks.registry/heat-chemistry-out-of-range?` /
  `turbine.registry/unit-tolerance-out-of-range?` /
  `automotive.registry/vehicle-emissions-out-of-range?` /
  `machinetool.registry/unit-accuracy-out-of-range?` /
  `heavyequip.registry/unit-out-of-range?` /
  `pressureequip.registry/unit-test-pressure-out-of-range?`
  established the priors), applying the SAME lo/hi bounds-comparison
  shape to a crane/hoist/forklift unit's own measured proof-load test
  result against the unit's own recorded spec bounds -- the direct
  analog of ASME B30's required proof-load test (typically 100-125%
  of rated capacity / Safe Working Load, SWL) and the code's
  overstress ceiling (a test load high enough to risk permanent
  deformation is out of range on the other side).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real fab/final-assembly control system. It builds the
  RECORD a manufacturer would keep, not the act of dispatching the
  robot unit action or issuing the load-test certificate itself (that
  is `liftingequip.operation`'s `:actuation/dispatch-unit`/
  `:actuation/issue-load-test-certificate`, always human-gated -- see
  README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  manufacturer's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn unit-test-load-out-of-range?
  "Does `unit`'s own `:test-load-actual` (proof-load acceptance test
  result) fall outside its own `[:test-load-min :test-load-max]`
  recorded spec-bounds? A pure ground-truth check against the unit's
  own permanent fields -- no upstream comparison needed. One of this
  fleet's two-sided range check family (see ns docstring)."
  [{:keys [test-load-actual test-load-min test-load-max]}]
  (and (number? test-load-actual) (number? test-load-min) (number? test-load-max)
       (or (< test-load-actual test-load-min)
           (> test-load-actual test-load-max))))

(defn register-unit-dispatch
  "Validate + construct the UNIT-DISPATCH registration DRAFT -- the
  manufacturer's own act of dispatching a real robot final-assembly/
  shipment action to complete a crane/hoist/forklift unit. Pure
  function -- does not touch any real fab/final-assembly control
  system; it builds the RECORD a manufacturer would keep.
  `liftingequip.governor` independently re-verifies the unit's own
  test-load sufficiency against its own spec bounds, and a
  double-dispatch for the same unit, before this is ever allowed to
  commit."
  [unit-id jurisdiction sequence]
  (when-not (and unit-id (not= unit-id ""))
    (throw (ex-info "unit-dispatch: unit_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "unit-dispatch: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "unit-dispatch: sequence must be >= 0" {})))
  (let [dispatch-number (str (str/upper-case jurisdiction) "-LEQ-" (zero-pad sequence 6))
        record {"record_id" dispatch-number
                "kind" "unit-dispatch-draft"
                "unit_id" unit-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "dispatch_number" dispatch-number
     "certificate" (unsigned-certificate "UnitDispatch" dispatch-number dispatch-number)}))

(defn register-load-test-certificate
  "Validate + construct the LOAD-TEST-CERTIFICATE registration DRAFT --
  the manufacturer's own act of issuing a real ASME B30/LOLER
  proof-load acceptance-test certificate documenting a unit's
  load-test result. Pure function -- does not touch any real
  fab/final-assembly control system; it builds the RECORD a
  manufacturer would keep. `liftingequip.governor` independently
  re-verifies the unit's own load-test defect resolution status, and
  a double-issuance for the same unit, before this is ever allowed to
  commit."
  [unit-id jurisdiction sequence]
  (when-not (and unit-id (not= unit-id ""))
    (throw (ex-info "load-test-certificate: unit_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "load-test-certificate: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "load-test-certificate: sequence must be >= 0" {})))
  (let [evidence-number (str (str/upper-case jurisdiction) "-LTC-" (zero-pad sequence 6))
        record {"record_id" evidence-number
                "kind" "load-test-certificate-draft"
                "unit_id" unit-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "evidence_number" evidence-number
     "certificate" (unsigned-certificate "LoadTestCertificate" evidence-number evidence-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
