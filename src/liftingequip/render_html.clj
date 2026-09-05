(ns liftingequip.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2: this repo previously shipped a
  HAND-WRITTEN `docs/samples/operator-console.html` (static markup with
  its own inline CSS, no generator, numbers typed by a human). This
  namespace replaces it with a page produced by actually running the
  REAL actor stack -- `liftingequip.operation` (langgraph StateGraph)
  -> `liftingequip.governor` -> `liftingequip.store` -- through a
  scenario adapted from this repo's own demo driver
  (`liftingequip.sim`, `clojure -M:dev:run`, run BEFORE writing this
  file to confirm it produces a sensible ledger against the real seeded
  unit ids `unit-1`..`unit-4`, which DO match
  `liftingequip.store/demo-data`).

  What is real vs. static, stated plainly:

    - The units table, the audit-ledger table, the draft
      unit-dispatch / load-test-certificate tables and every count in
      the summary are RUNTIME OUTPUT of `run-demo!` below. Nothing in
      them is hand-typed -- the hold rules, the reference numbers
      (`JPN-LEQ-000000` / `JPN-LTC-000000`) and the dispositions are
      whatever the governor and the registry actually produced.
    - `action-gate-rows` is a STATIC DESCRIPTION of this actor's fixed
      op contract (README `Ops`, `liftingequip.phase/phases`,
      `liftingequip.governor/high-stakes`). It describes behaviour that
      is a permanent structural fact of the code, not telemetry from
      this run, so it is legitimately hand-described.

  Deterministic by construction: no timestamps, no randomness, no
  wall-clock anywhere in the page body. Two consecutive runs against
  the same seed are byte-identical (verify by diffing two runs).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [clojure.string :as str]
            [liftingequip.store :as store]
            [liftingequip.registry :as registry]
            [liftingequip.export :as export]
            [liftingequip.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator
  "The human lifting-equipment manufacturing engineer this demo runs
  as. Phase 3 (`supervised-auto`) is the most permissive phase this
  actor has -- and even there `:actuation/dispatch-unit` /
  `:actuation/issue-load-test-certificate` never auto-commit."
  {:actor-id "op-1" :actor-role :lifting-equipment-engineer :phase 3})

(defn- exec!
  "One operation = one supervised actor run, exactly as
  `liftingequip.sim` drives it."
  [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve!
  "Resume a run paused by `interrupt-before #{:request-approval}` with a
  human approval, exactly as `liftingequip.sim` does."
  [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario that reaches every
  disposition this actor can produce, and returns the store. Every
  value the page renders comes out of this run.

  `unit-1` (JPN, test load 11.0 inside its own [10.0,12.5] spec
  window, no unresolved load-test defect) clears the full lifecycle:
  intake (auto-commits -- the ONLY op in phase 3's `:auto` set, no
  capital risk), a per-jurisdiction design-rules/conformity evidence
  verification (phase-gated, not auto-eligible -> approved), a
  proof-load-test screening (approved), a robot unit dispatch (ALWAYS
  escalates -- `:actuation/dispatch-unit` is permanently high-stakes,
  absent from every phase's `:auto` set -> approved) and a load-test
  certificate issuance (same posture -> approved).

  Then five distinct HARD governor holds, none of which ever reaches a
  human at all (a human approver cannot override any of them):

    :no-spec-basis              -- `unit-2` sits in jurisdiction \"ATL\",
                                   deliberately absent from
                                   `liftingequip.facts/catalog`, so the
                                   advisor returns a proposal with no
                                   spec-basis citation rather than
                                   inventing that jurisdiction's
                                   requirements.
    :unit-test-load-out-of-range -- `unit-3`'s own measured proof-load
                                   acceptance test result (15.0) falls
                                   outside its own recorded spec bounds
                                   [10.0,12.5]; the governor recomputes
                                   this independently via
                                   `liftingequip.registry/unit-test-load-
                                   out-of-range?`, never trusting the
                                   proposal. (`unit-3` first clears its
                                   own design-rules verification, so the
                                   hold is unambiguously the range check
                                   and not missing evidence.)
    :load-test-defect-unresolved -- `unit-4`'s proof-load-test screening
                                   itself detects an unresolved defect,
                                   and HARD-holds on its own finding.
    :already-dispatched          -- a second `:actuation/dispatch-unit`
                                   against `unit-1`, refused off the
                                   dedicated `:unit-dispatched?` fact.
    :already-certified           -- a second `:actuation/issue-load-test-
                                   certificate` against `unit-1`,
                                   refused off `:load-test-certified?`."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    ;; --- unit-1: full clean lifecycle ---
    (exec! actor "u1-intake" {:op :unit/intake :subject "unit-1"
                              :patch {:id "unit-1"
                                      :unit-name "Sakura 10t Overhead Bridge Crane OC-04"}})

    (exec! actor "u1-verify" {:op :design-rules/verify :subject "unit-1"})
    (approve! actor "u1-verify")

    (exec! actor "u1-screen" {:op :load-test/screen :subject "unit-1"})
    (approve! actor "u1-screen")

    (exec! actor "u1-dispatch" {:op :actuation/dispatch-unit :subject "unit-1"})
    (approve! actor "u1-dispatch")

    (exec! actor "u1-certify" {:op :actuation/issue-load-test-certificate :subject "unit-1"})
    (approve! actor "u1-certify")

    ;; --- HARD hold: no official spec-basis for the jurisdiction ---
    (exec! actor "u2-verify" {:op :design-rules/verify :subject "unit-2" :no-spec? true})

    ;; --- HARD hold: measured proof-load result outside the unit's own window ---
    (exec! actor "u3-verify" {:op :design-rules/verify :subject "unit-3"})
    (approve! actor "u3-verify")
    (exec! actor "u3-dispatch" {:op :actuation/dispatch-unit :subject "unit-3"})

    ;; --- HARD hold: the screening op holds on its own unresolved finding ---
    (exec! actor "u4-screen" {:op :load-test/screen :subject "unit-4"})

    ;; --- HARD holds: double dispatch / double certificate issuance ---
    (exec! actor "u1-dispatch-again" {:op :actuation/dispatch-unit :subject "unit-1"})
    (exec! actor "u1-certify-again" {:op :actuation/issue-load-test-certificate :subject "unit-1"})
    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- last-fact-for [ledger unit-id]
  (last (filter #(= (:subject %) unit-id) ledger)))

(defn- status-cell
  "The unit's LAST ledger fact this run, rendered as a status. Real
  runtime output -- the rule name shown on a hold is the governor's own
  `:violations` entry."
  [ledger unit-id]
  (let [f (last-fact-for ledger unit-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :governor-hold (:t f))
      (let [rule (-> f :violations first :rule)]
        (str "<span class=\"critical\">HARD hold &middot; "
             (esc (name (or rule :unknown))) "</span>"))
      (= :approval-rejected (:t f)) "<span class=\"critical\">rejected by approver</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- test-load-cell
  "Proof-load acceptance test result against the unit's OWN recorded
  spec window, classified with the same pure predicate the governor
  uses (`liftingequip.registry/unit-test-load-out-of-range?`) -- not a
  second, hand-written comparison."
  [{:keys [test-load-actual test-load-min test-load-max] :as unit}]
  (let [out? (registry/unit-test-load-out-of-range? unit)
        window (str (esc test-load-actual) " "
                    (if out? "&notin;" "&isin;")
                    " [" (esc test-load-min) "," (esc test-load-max) "]")]
    (str "<span class=\"" (if out? "critical" "ok") "\">" window "</span>")))

(defn- load-test-cell
  "Committed proof-load-test screening verdict on file for the unit, if
  the screening op actually committed one (a screening that HARD-held
  writes no verdict -- the store stays empty, and the page says so)."
  [db {:keys [id load-test-defect-unresolved?]}]
  (let [verdict (:verdict (store/load-screen-of db id))]
    (cond
      (= :resolved verdict) "<span class=\"ok\">resolved</span>"
      (= :unresolved verdict) "<span class=\"critical\">unresolved</span>"
      load-test-defect-unresolved? "<span class=\"critical\">defect unresolved &middot; not screened-in</span>"
      :else "<span class=\"muted\">not screened</span>")))

(defn- actuation-cell
  "The two independent actuation lifecycles, off their own dedicated
  booleans (never a `:status` value)."
  [{:keys [unit-dispatched? load-test-certified? dispatch-number evidence-number]}]
  (cond
    (and unit-dispatched? load-test-certified?)
    (str "<span class=\"ok\">dispatched " (esc dispatch-number)
         " &middot; certified " (esc evidence-number) "</span>")
    unit-dispatched?
    (str "<span class=\"warn\">dispatched " (esc dispatch-number)
         " &middot; not yet certified</span>")
    load-test-certified?
    (str "<span class=\"warn\">certified " (esc evidence-number)
         " &middot; not dispatched</span>")
    :else "<span class=\"muted\">in build</span>"))

(defn- unit-row [db ledger {:keys [id unit-name jurisdiction] :as unit}]
  (format "        <tr><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id) (esc unit-name) (esc jurisdiction)
          (test-load-cell unit)
          (load-test-cell db unit)
          (actuation-cell unit)
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis violations]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t))
          (esc (name (or op :n-a)))
          (esc subject)
          (esc (or (some-> disposition name) ""))
          (esc (or (some->> violations (map (comp name :rule)) seq (str/join ", "))
                   (some->> basis (map name) (str/join ", "))
                   ""))))

(defn- draft-row [r]
  (format "        <tr><td><code>%s</code></td><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (get r "record_id")) (esc (get r "kind"))
          (esc (get r "unit_id")) (esc (get r "jurisdiction"))
          (if (get r "immutable")
            "<span class=\"ok\">immutable</span>"
            "<span class=\"muted\">mutable</span>")))

(def ^:private action-gate-rows
  ;; STATIC description of this actor's own closed op contract
  ;; (README `Ops`, `liftingequip.phase/phases`,
  ;; `liftingequip.governor/high-stakes`) -- documentation of fixed
  ;; behaviour, not runtime telemetry from this run, so it is
  ;; legitimately hand-described rather than derived from the ledger.
  ["        <tr><td><code>:unit/intake</code></td><td><span class=\"ok\">phase-3 auto-commit when governor-clean &middot; no capital risk</span></td></tr>"
   "        <tr><td><code>:design-rules/verify</code></td><td><span class=\"warn\">phase-3: human approval (not auto-eligible) &middot; HARD hold with no official spec-basis</span></td></tr>"
   "        <tr><td><code>:load-test/screen</code></td><td><span class=\"warn\">phase-3: human approval (not auto-eligible) &middot; HARD hold on its own unresolved finding</span></td></tr>"
   "        <tr><td><code>:actuation/dispatch-unit</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; proof-load window recomputed independently &middot; double dispatch refused</span></td></tr>"
   "        <tr><td><code>:actuation/issue-load-test-certificate</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; unresolved load-test defect blocks outright &middot; double issuance refused</span></td></tr>"])

(defn render
  "Renders the whole operator-console document from a store `db` that
  has already been driven by `run-demo!` (or any other real scenario).
  Pure: `db` in, HTML string out, no I/O and nothing time-dependent."
  [db]
  (let [ledger (vec (store/ledger db))
        units (store/all-units db)
        counts (:counts (export/audit-package db))
        holds (filter #(= :governor-hold (:t %)) ledger)
        commits (filter #(= :committed (:t %)) ledger)
        unit-rows (str/join "\n" (map (partial unit-row db ledger) units))
        ledger-rows (str/join "\n" (map ledger-row ledger))
        dispatch-rows (str/join "\n" (map draft-row (store/dispatch-history db)))
        evidence-rows (str/join "\n" (map draft-row (store/evidence-history db)))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-2816 &middot; lifting-equipment plant</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Lifting &amp; handling equipment manufacturing (ISIC 2816) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · never dispatches hardware</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Units</h2>\n"
     "    <p class=\"muted\">Build-time snapshot — generated by running the real actor "
     "(<code>liftingequip.operation</code> → <code>liftingequip.governor</code> → "
     "<code>liftingequip.store</code>) via <code>clojure -M:dev:render-html</code>. "
     "Every cell below is runtime output; nothing here is hand-typed.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Unit</th><th>Name</th><th>Jurisdiction</th><th>Proof-load test (t)</th>"
     "<th>Load-test screening</th><th>Actuation lifecycle</th><th>Last decision</th></tr></thead>\n"
     "      <tbody>\n"
     unit-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "    <p class=\"muted\">"
     (esc (:units counts)) " units · "
     (esc (:ledger counts)) " ledger facts ("
     (esc (count commits)) " committed, " (esc (count holds)) " HARD holds) · "
     (esc (:dispatches counts)) " draft unit dispatch(es) · "
     (esc (:load-test-certificates counts)) " draft load-test certificate(s).</p>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Lifting Equipment Governor)</h2>\n"
     "    <p class=\"muted\">Fixed contract of this actor, not telemetry. HARD holds cannot be "
     "overridden by any human approver. A unit's measured proof-load acceptance test result is "
     "recomputed independently from the unit's own recorded spec bounds and never trusted from the "
     "proposal; an unresolved load-test defect blocks certificate issuance outright; each unit can be "
     "dispatched once and certified once.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every commit and every hold this "
     "scenario actually produced, in order. Holds show the governor's own violated rule.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Unit</th><th>Disposition</th><th>Basis / violated rule</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Draft unit-dispatch records</h2>\n"
     "    <p class=\"muted\">Unsigned drafts built by <code>liftingequip.registry</code>. This actor "
     "does not talk to plant control systems — signature and hardware dispatch are the manufacturer's "
     "own acts.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Record</th><th>Kind</th><th>Unit</th><th>Jurisdiction</th><th>Append-only</th></tr></thead>\n"
     "      <tbody>\n"
     dispatch-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Draft load-test-certificate records</h2>\n"
     "    <p class=\"muted\">Unsigned ASME B30 / LOLER 1998 proof-load acceptance-test certificate "
     "drafts. Issuance is always a human lifting-equipment engineer's call.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Record</th><th>Kind</th><th>Unit</th><th>Jurisdiction</th><th>Append-only</th></tr></thead>\n"
     "      <tbody>\n"
     evidence-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "<footer>cloud-itonami-isic-2816 · AGPL-3.0-or-later · regenerate with "
     "<code>clojure -M:dev:render-html</code></footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)
        {:keys [units ledger dispatches load-test-certificates]}
        (:counts (export/audit-package db))]
    (spit out html)
    (println "wrote" out "(" units "units," ledger "ledger facts,"
             dispatches "draft dispatch(es)," load-test-certificates
             "draft load-test certificate(s) )")))
