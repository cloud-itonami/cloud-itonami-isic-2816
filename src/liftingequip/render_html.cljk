(ns liftingequip.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2. This repo previously shipped a
  HAND-WRITTEN `docs/samples/operator-console.html` (static markup,
  its own inline CSS, numbers typed by a human, no generator at all).
  This namespace replaces it with a page produced by actually running
  the REAL actor stack -- `liftingequip.operation` (a langgraph-clj
  StateGraph) -> `liftingequip.governor` -> `liftingequip.phase` ->
  `liftingequip.store` -- over this repo's own seeded unit directory
  (`liftingequip.store/demo-data`, unit ids `unit-1`..`unit-4`).

  What is derived vs. hand-written, stated plainly:

    - Every unit, id, name, jurisdiction, test-load figure, hold rule,
      hold detail, confidence, dispatch/certificate reference number
      and ledger row on the page is RUNTIME OUTPUT of `run-demo!`
      below. Nothing in those tables is typed by a human here.
    - The operation journal reports the actual `langgraph.graph/run*`
      `:status` of each run (`:interrupted` is the human-in-the-loop
      pause really happening, not a claim about it) and the actual
      `:disposition` the graph reached.
    - The op-gate table is derived from `liftingequip.phase/phases`,
      `liftingequip.phase/write-ops` and
      `liftingequip.governor/high-stakes` -- read out of the vars, not
      re-described in prose here, so it cannot drift from the code.
    - The approver-attribution table is MEASURED: for each approval
      this run granted, the page reads the record back out of the
      store and reports whether the approver id survived. It is not a
      hard-coded claim, so it self-corrects if the store is fixed.
    - Prose (section intros, footer) is hand-written, as prose is.

  Deterministic by construction: no timestamp, no clock read, no
  randomness, every collection explicitly ordered. Two consecutive
  runs are byte-identical.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`). `-main` REFUSES to
  write the file when the run produced no HARD governor hold -- a
  console showing a governor that never said no would be a lie about
  this actor even if every byte of it were generated."
  (:require [jp-go-dds.skin]
            [kotoba.lang.text :as str]
            [langgraph.graph :as g]
            [liftingequip.export :as export]
            [liftingequip.facts :as facts]
            [liftingequip.governor :as governor]
            [liftingequip.operation :as op]
            [liftingequip.phase :as phase]
            [liftingequip.registry :as registry]
            [liftingequip.store :as store]))

;; ----------------------------- driving the real actor -----------------------------

(def ^:private operator
  "The human lifting-equipment manufacturing engineer this demo runs
  as, at phase 3 (`supervised-auto`) -- the most permissive phase this
  actor has, where `:actuation/dispatch-unit` and
  `:actuation/issue-load-test-certificate` STILL never auto-commit."
  {:actor-id "op-1" :actor-role :lifting-equipment-engineer :phase 3})

(def ^:private early-rollout-operator
  "The same engineer during an earlier rollout stage (phase 1,
  `assisted-intake`), used once below to exercise the rollout phase
  gate itself -- a hold with no governor violation at all."
  (assoc operator :phase 1))

(defn- exec!
  "One operation = one supervised actor run, driven exactly as
  `liftingequip.sim` drives it. Records what actually came back."
  [actor tid request ctx]
  (let [r (g/run* actor {:request request :context ctx} {:thread-id tid})]
    {:thread      tid
     :op          (:op request)
     :subject     (:subject request)
     :phase       (:phase ctx)
     :actor       (:actor-id ctx)
     :status      (:status r)
     :disposition (get-in r [:state :disposition])
     :confidence  (get-in r [:state :verdict :confidence])
     :violations  (get-in r [:state :verdict :violations])}))

(defn- resume!
  "Resume a run paused by `interrupt-before #{:request-approval}` with a
  real human decision (`:approved` / `:rejected`), and record the
  record the graph produced so the page can check what the store kept."
  [journal-entry actor tid decision by]
  (let [r (g/run* actor {:approval {:status decision :by by}}
                  {:thread-id tid :resume? true})]
    (assoc journal-entry
           :approval        decision
           :approval-by     by
           :status-after    (:status r)
           :disposition     (get-in r [:state :disposition])
           :record-effect   (get-in r [:state :record :effect])
           :record-payload  (get-in r [:state :record :payload]))))

(defn run-demo!
  "Drives a fresh seeded store through a scenario that reaches EVERY
  disposition this actor can produce, and returns `{:db .. :journal ..}`.

  The units are this repo's own seed data, not invented here:

    unit-1  JPN  test load 11.0 inside its own [10.0,12.5] spec window,
                 no unresolved load-test defect  -> the clean lifecycle
    unit-2  ATL  a jurisdiction genuinely ABSENT from
                 `liftingequip.facts/catalog` -- no `:no-spec?` flag is
                 needed, the seed jurisdiction alone produces the hold
    unit-3  JPN  test load 15.0, OUTSIDE its own [10.0,12.5] window
    unit-4  JPN  `:load-test-defect-unresolved? true`

  unit-1 clears the full lifecycle: intake (the ONLY op in phase 3's
  `:auto` set, so it auto-commits), a per-jurisdiction design-rules
  evidence verification (phase-gated -> approved), a proof-load-test
  screening (approved), a robot unit dispatch (ALWAYS escalates --
  permanently high-stakes, absent from every phase's `:auto` set ->
  approved) and a load-test-certificate issuance (same posture ->
  approved).

  Then all SIX of the governor's HARD rules fire, none of which ever
  reaches a human -- `:no-spec-basis` (unit-2), `:unit-test-load-out-
  of-range` (unit-3), `:load-test-defect-unresolved` (unit-4 screened
  DIRECTLY via `:load-test/screen`, never via an actuation op against
  an unscreened unit), `:evidence-incomplete` (a dispatch attempt on
  unit-4, which has no requirements verification on file at all),
  `:already-dispatched` and `:already-certified` (unit-1 a second
  time).

  Finally two holds that are NOT hard, to show the difference: a
  screening of unit-2 that the human engineer escalation REJECTS, and
  a phase-1 verification attempt held by the rollout phase gate with
  no governor violation at all."
  []
  (let [db    (store/seed-db)
        actor (op/build db)
        unit1 (store/unit db "unit-1")
        j     (atom [])
        step! (fn [tid request ctx] (let [e (exec! actor tid request ctx)]
                                      (swap! j conj e) e))
        decide! (fn [tid request ctx decision]
                  (let [e (exec! actor tid request ctx)
                        e (resume! e actor tid decision (:actor-id ctx))]
                    (swap! j conj e) e))]

    ;; --- unit-1: the full clean lifecycle -------------------------------
    (step! "t01" {:op :unit/intake :subject "unit-1"
                  :patch {:id "unit-1" :unit-name (:unit-name unit1)}} operator)
    (decide! "t02" {:op :design-rules/verify :subject "unit-1"} operator :approved)
    (decide! "t03" {:op :load-test/screen :subject "unit-1"} operator :approved)
    (decide! "t04" {:op :actuation/dispatch-unit :subject "unit-1"} operator :approved)
    (decide! "t05" {:op :actuation/issue-load-test-certificate :subject "unit-1"} operator :approved)

    ;; --- the six HARD holds ---------------------------------------------
    (step! "t06" {:op :design-rules/verify :subject "unit-2"} operator)
    (decide! "t07" {:op :design-rules/verify :subject "unit-3"} operator :approved)
    (step! "t08" {:op :actuation/dispatch-unit :subject "unit-3"} operator)
    (step! "t09" {:op :load-test/screen :subject "unit-4"} operator)
    (step! "t10" {:op :actuation/dispatch-unit :subject "unit-4"} operator)
    (step! "t11" {:op :actuation/dispatch-unit :subject "unit-1"} operator)
    (step! "t12" {:op :actuation/issue-load-test-certificate :subject "unit-1"} operator)

    ;; --- holds that are NOT hard -----------------------------------------
    (decide! "t13" {:op :load-test/screen :subject "unit-2"} operator :rejected)
    (step! "t14" {:op :design-rules/verify :subject "unit-4"} early-rollout-operator)

    {:db db :journal @j}))

;; ----------------------------- reading the run back -----------------------------

(defn hard-holds
  "The HARD governor holds on the ledger: a `:governor-hold` fact
  carrying at least one governor rule violation. These are the holds a
  human approver cannot override -- they never reach a human at all.
  `-main` refuses to write a console when this is empty."
  [db]
  (filterv #(and (= :governor-hold (:t %)) (seq (:violations %))) (store/ledger db)))

(defn- phase-gate-holds
  "Holds written by the rollout phase gate rather than by a governor
  rule: a `:governor-hold` fact with no violations on it."
  [db]
  (filterv #(and (= :governor-hold (:t %)) (empty? (:violations %))) (store/ledger db)))

(defn- rejected-holds
  "Escalations that DID reach a human, and the human said no."
  [db]
  (filterv #(= :approval-rejected (:t %)) (store/ledger db)))

(defn- key-name [k] (cond (keyword? k) (name k) (string? k) k :else (str k)))

(def ^:private approver-key-names
  "Field names any layer of this actor could plausibly use to retain
  the human approver's id."
  #{"approved-by" "approved_by" "approver" "by"})

(defn- approver-in
  "The approver id retained anywhere in `m` (keyword- or string-keyed),
  or nil. Used to MEASURE where an approval actually lands rather than
  asserting it."
  [m]
  (when (map? m)
    (some (fn [[k v]] (when (approver-key-names (key-name k)) v)) m)))

(defn- record-for-effect
  "Read the record a committed effect produced back OUT of the store,
  so the page reports what the SSoT actually kept."
  [db effect subject]
  (case effect
    :verification/set     (store/requirements-verification-of db subject)
    :load-test-screen/set (store/load-screen-of db subject)
    :unit/upsert          (store/unit db subject)
    :unit/mark-dispatched (first (filter #(= subject (get % "unit_id")) (store/dispatch-history db)))
    :unit/mark-certified  (first (filter #(= subject (get % "unit_id")) (store/evidence-history db)))
    nil))

(defn- ledger-fact-for
  [db op subject]
  (last (filter #(and (= op (:op %)) (= subject (:subject %))) (store/ledger db))))

(defn approver-attribution
  "For every approval this run granted, measure where the approver id
  ended up: in the record the actor handed to the store, in what the
  store kept, and in the audit ledger. Derived at render time, so a
  later fix to `store/commit-record!` shows up here without editing
  this namespace."
  [db journal]
  (mapv (fn [{:keys [op subject approval-by record-effect record-payload]}]
          (let [stored (record-for-effect db record-effect subject)
                fact   (ledger-fact-for db op subject)]
            {:op op
             :subject subject
             :effect record-effect
             :approver approval-by
             :in-actor-record (approver-in record-payload)
             :in-store        (approver-in stored)
             :in-ledger       (approver-in fact)}))
        (filter #(= :approved (:approval %)) journal)))

(defn op-gate-rows
  "The op contract, READ OUT of `liftingequip.phase` and
  `liftingequip.governor` rather than re-described in prose: which
  phases may write each op, which phases may auto-commit it, and
  whether it is permanently high-stakes."
  []
  (let [phase-ids (sort (keys phase/phases))]
    (mapv (fn [op]
            {:op op
             :writes-at (filterv #(contains? (:writes (phase/phases %)) op) phase-ids)
             :auto-at   (filterv #(contains? (:auto (phase/phases %)) op) phase-ids)
             :high-stakes? (boolean (governor/high-stakes op))})
          (sort-by str phase/write-ops))))

;; ----------------------------- html -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- code [v] (str "<code>" (esc v) "</code>"))

(defn- span [class v] (str "<span class=\"" class "\">" v "</span>"))

(defn- yes-no [b yes no]
  (if b (span "critical" (esc yes)) (span "muted" (esc no))))

(defn- tr [cells]
  (str "        <tr>" (str/join (map #(str "<td>" % "</td>") cells)) "</tr>"))

(defn- table [headers rows]
  (str "    <table>\n"
       "      <thead><tr>" (str/join (map #(str "<th>" (esc %) "</th>") headers)) "</tr></thead>\n"
       "      <tbody>\n"
       (str/join "\n" rows) "\n"
       "      </tbody>\n"
       "    </table>\n"))

(defn- section [title intro body]
  (str "  <section class=\"card\">\n"
       "    <h2>" (esc title) "</h2>\n"
       "    <p class=\"muted\">" intro "</p>\n"
       body
       "  </section>\n"))

(defn- ops-list [ops]
  (if (seq ops) (str/join ", " (map str ops)) "—"))

;; --- individual row builders (every value below comes off the run) ---

(defn- unit-row [{:keys [id unit-name jurisdiction test-load-actual test-load-min test-load-max
                         load-test-defect-unresolved? unit-dispatched? load-test-certified?
                         dispatch-number evidence-number] :as unit}]
  (let [out? (registry/unit-test-load-out-of-range? unit)]
    (tr [(code id)
         (esc unit-name)
         (esc jurisdiction)
         (str (span "num" (esc test-load-actual)) " t &nbsp;"
              (span "muted" (str "spec [" (esc test-load-min) ", " (esc test-load-max) "]"))
              " &nbsp;"
              (if out? (span "critical" "out of range") (span "ok" "in range")))
         (if load-test-defect-unresolved?
           (span "critical" "unresolved defect")
           (span "ok" "no unresolved defect"))
         (if unit-dispatched?
           (str (span "ok" "dispatched") " " (span "num" (esc dispatch-number)))
           (span "muted" "not dispatched"))
         (if load-test-certified?
           (str (span "ok" "certified") " " (span "num" (esc evidence-number)))
           (span "muted" "not certified"))])))

(defn- journal-row [{:keys [thread op subject phase status disposition approval approval-by
                            status-after confidence]}]
  (tr [(code thread)
       (code op)
       (code subject)
       (span "num" (esc phase))
       (str (code status)
            (when status-after (str " &rarr; " (code status-after))))
       (case approval
         :approved (span "ok" (str "approved by " (esc approval-by)))
         :rejected (span "critical" (str "REJECTED by " (esc approval-by)))
         (span "muted" "no human reached"))
       (case disposition
         :commit (span "ok" "commit")
         :hold   (span "critical" "hold")
         :escalate (span "warn" "escalate")
         (span "muted" (esc disposition)))
       (span "num" (esc (if (nil? confidence) "—" confidence)))]))

(defn- hold-row [{:keys [op subject violations confidence]}]
  (let [v (first violations)]
    (tr [(code op)
         (code subject)
         (span "critical" (esc (name (:rule v))))
         (esc (:detail v))
         (span "num" (esc confidence))])))

(defn- soft-hold-row [{:keys [t op subject phase phase-reason violations]}]
  (tr [(code t)
       (code op)
       (code subject)
       (if phase (span "num" (esc phase)) (span "muted" "—"))
       (esc (or (some-> phase-reason name)
                (some-> violations first :rule name)
                "—"))
       (if (= :approval-rejected t)
         (span "warn" "reached a human, who refused")
         (span "warn" "never reached a human (rollout stage)"))]))

(defn- gate-row [{:keys [op writes-at auto-at high-stakes?]}]
  (tr [(code op)
       (span "num" (ops-list writes-at))
       (if (seq auto-at)
         (span "ok" (esc (ops-list auto-at)))
         (span "critical" "never, at any phase"))
       (yes-no high-stakes? "always human" "no")]))

(defn- ledger-row [i {:keys [t op actor subject disposition basis]}]
  (tr [(span "num" (esc i))
       (case t
         :committed (span "ok" (esc (name t)))
         :governor-hold (span "critical" (esc (name t)))
         :approval-rejected (span "warn" (esc (name t)))
         (esc (str t)))
       (code op)
       (code subject)
       (esc actor)
       (esc (name (or disposition :n-a)))
       (esc (if (seq basis) (str/join " / " (map str basis)) "—"))]))

(defn- draft-row [{:strs [record_id kind unit_id jurisdiction immutable]}]
  (tr [(span "num" (esc record_id))
       (code kind)
       (code unit_id)
       (esc jurisdiction)
       (if immutable (span "ok" "immutable") (span "muted" "—"))
       (span "muted" "unsigned draft — signature is the manufacturer's own act")]))

(defn- attribution-row [{:keys [op subject effect approver in-actor-record in-store in-ledger]}]
  (tr [(code op)
       (code subject)
       (code effect)
       (esc approver)
       (if in-actor-record (span "ok" (esc in-actor-record)) (span "critical" "dropped"))
       (if in-store (span "ok" (esc in-store)) (span "critical" "not retained"))
       (if in-ledger (span "ok" (esc in-ledger)) (span "critical" "not retained"))]))

(defn- jurisdiction-row [iso3 covered?]
  (let [sb (facts/spec-basis iso3)]
    (tr [(code iso3)
         (if covered? (esc (:name sb)) (span "muted" "—"))
         (if covered?
           (span "ok" "spec-basis on file")
           (span "critical" "NO spec-basis — requirements may not be invented"))
         (if covered? (esc (:owner-authority sb)) (span "muted" "—"))
         (if covered? (esc (:legal-basis sb)) (span "muted" "—"))
         (if covered?
           (span "num" (esc (count (facts/evidence-checklist iso3))))
           (span "muted" "0"))])))

(defn- count-row [label n]
  (tr [(esc label) (span "num" (esc n))]))

;; ----------------------------- the page -----------------------------

(defn render
  "Renders the whole console from `{:db .. :journal ..}` as returned by
  `run-demo!`. Pure: no I/O, no clock, no randomness -- given the same
  run it returns the same string, byte for byte."
  [{:keys [db journal]}]
  (let [ledger      (vec (store/ledger db))
        units       (store/all-units db)
        hard        (hard-holds db)
        soft        (concat (rejected-holds db) (phase-gate-holds db))
        rules       (sort (distinct (map #(-> % :violations first :rule) hard)))
        jurisdictions (sort (distinct (map :jurisdiction units)))
        coverage    (facts/coverage jurisdictions)
        covered     (set (:covered-jurisdictions coverage))
        pkg         (export/audit-package db)
        bundle      (export/package->csv-bundle db)
        attribution (approver-attribution db journal)
        retained    (count (filter :in-store attribution))]
    (str
     "<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
     "<title>cloud-itonami-isic-2816 &middot; lifting &amp; handling equipment manufacturing &mdash; Operator Console</title>"
     "<style>\n" (jp-go-dds.skin/dds+skin) "\n</style></head><body>\n"

     "<header class=\"bar\">\n"
     "  <h1>Lifting &amp; handling equipment manufacturing (ISIC 2816) &mdash; Operator Console</h1>\n"
     "  <span class=\"badge\">build-time sample &middot; governor-gated &middot; dispatch &amp; load-test certificates always human-approved</span>\n"
     "</header>\n"
     "<main>\n"

     "  <section class=\"banner\">\n"
     "    <p>Every number, id, hold reason and reference number below is <strong>runtime output</strong> of the real "
     "actor stack &mdash; <code>liftingequip.operation</code> (a langgraph StateGraph) &rarr; "
     "<code>liftingequip.governor</code> &rarr; <code>liftingequip.phase</code> &rarr; <code>liftingequip.store</code> "
     "&mdash; driven over this repo&rsquo;s own seeded unit directory by <code>liftingequip.render-html/run-demo!</code>. "
     "This run produced <strong>" (count ledger) "</strong> audit-ledger facts, "
     "<strong>" (count hard) "</strong> HARD governor holds across "
     "<strong>" (count rules) "</strong> distinct governor rules, "
     "<strong>" (count (:dispatches pkg)) "</strong> unit-dispatch draft(s) and "
     "<strong>" (count (:load-test-certificates pkg)) "</strong> load-test-certificate draft(s). "
     "The generator refuses to write this file if the run produces zero HARD holds.</p>\n"
     "  </section>\n"

     (section "Units under management"
              (str "Straight out of <code>liftingequip.store/all-units</code> after the run. "
                   "The in-range / out-of-range verdict is recomputed here by "
                   "<code>liftingequip.registry/unit-test-load-out-of-range?</code> &mdash; the same independent "
                   "ground-truth check the governor makes, never a value copied from a proposal.")
              (table ["Unit" "Name" "Jurisdiction" "Proof-load test result" "Load-test defect"
                      "Dispatch" "Load-test certificate"]
                     (map unit-row units)))

     (section "Operation journal (this run)"
              (str "One row per supervised actor run, in execution order. <code>status</code> is the actual "
                   "<code>langgraph.graph/run*</code> result: <code>:interrupted</code> is the "
                   "<code>interrupt-before #{:request-approval}</code> pause really happening, and the arrow shows "
                   "the status after a human resumed it. Confidence is the advisor&rsquo;s self-reported figure as "
                   "the governor saw it &mdash; never the basis for an actuation on its own.")
              (table ["Thread" "Op" "Subject" "Phase" "Run status" "Human" "Disposition" "Confidence"]
                     (map journal-row journal)))

     (section (str "HARD governor holds (" (count hard) ")")
              (str "A HARD hold is un-overridable: no human approver is ever asked, because you do not get to "
                   "approve your way past a fabricated design-rules spec-basis, an incomplete evidence file, an "
                   "out-of-window proof-load test, an unresolved load-test defect, or a second dispatch / second "
                   "certificate for the same unit. Rules exercised by this run: "
                   (str/join ", " (map #(code %) rules)) ".")
              (table ["Op" "Unit" "Rule" "Detail (governor's own words)" "Advisor confidence"]
                     (map hold-row hard)))

     (section (str "Holds that are NOT hard (" (count soft) ")")
              (str "The contrast that makes the table above meaningful. A rollout-phase hold blocks an op the "
                   "current phase has not enabled yet, with no governor violation at all; a rejected escalation is "
                   "an op that <em>did</em> reach a human engineer, who refused it. Both land on the same "
                   "append-only ledger.")
              (table ["Fact" "Op" "Unit" "Phase" "Reason" "Who saw it"]
                     (map soft-hold-row soft)))

     (section "Op gate contract"
              (str "Read out of <code>liftingequip.phase/phases</code>, "
                   "<code>liftingequip.phase/write-ops</code> and "
                   "<code>liftingequip.governor/high-stakes</code> at render time, so this table cannot drift "
                   "from the code. Note that the two actuation ops are absent from every phase&rsquo;s "
                   "<code>:auto</code> set &mdash; a permanent structural fact, not a rollout milestone still to "
                   "come, and the governor&rsquo;s high-stakes gate enforces the same invariant independently.")
              (table ["Op" "May write at phases" "May auto-commit at phases" "High-stakes"]
                     (map gate-row (op-gate-rows))))

     (section (str "Audit ledger (" (count ledger) " facts)")
              (str "The append-only decision log <code>liftingequip.store/ledger</code> holds after the run: "
                   "every commit and every hold, in order, with the basis the governor recorded.")
              (table ["#" "Fact" "Op" "Unit" "Actor" "Disposition" "Basis"]
                     (map-indexed ledger-row ledger)))

     (section "Draft records produced"
              (str "Built by <code>liftingequip.registry</code> during the two approved actuations. This actor "
                   "does not touch a real fab or final-assembly control system and does not sign anything: it "
                   "builds the record a manufacturer would keep, and every certificate it emits is "
                   "<code>draft-unsigned</code>.")
              (table ["Reference" "Kind" "Unit" "Jurisdiction" "Ledger property" "Signature"]
                     (map draft-row (concat (store/dispatch-history db) (store/evidence-history db)))))

     (section "Spec-basis coverage for the jurisdictions in this directory"
              (str "<code>liftingequip.facts/coverage</code> over the jurisdictions the seeded units actually "
                   "carry: " (span "num" (esc (:covered coverage))) " of "
                   (span "num" (esc (:requested coverage))) " covered. Missing coverage is reported, never "
                   "papered over &mdash; that is exactly what the <code>:no-spec-basis</code> hold above is "
                   "protecting. " (esc (:note coverage)))
              (table ["ISO3" "Jurisdiction" "Spec-basis" "Owner authority" "Legal basis" "Required evidence items"]
                     (map #(jurisdiction-row % (contains? covered %)) jurisdictions)))

     ;; NB: section titles go through `esc`, so this heading carries a literal
     ;; U+2019 rather than an `&rsquo;` entity -- the entity would be
     ;; double-escaped and render as the seven characters `&rsquo;`.
     (section "Where does the approver’s id actually land?"
              (str "Measured, not asserted: for each approval this run granted, the page reads the record back "
                   "out of the store and reports whether the human approver&rsquo;s id survived. "
                   (if (= retained (count attribution))
                     (str "All " (count attribution) " approvals retained it.")
                     (str (span "critical"
                                (str "Only " retained " of " (count attribution)
                                     " approvals retain the approver in the SSoT."))
                          " The actor hands <code>:approved-by</code> to the store on the record&rsquo;s "
                          "<code>:payload</code>, but <code>store/commit-record!</code> only persists the payload "
                          "for the effects that store a payload; the two actuation effects rebuild their record "
                          "from the registry and keep no approver, and the <code>:committed</code> ledger fact "
                          "carries none either. So for a dispatch or a certificate this console can prove that a "
                          "human approved &mdash; the run paused at <code>:request-approval</code> and only moved "
                          "on when resumed &mdash; but it cannot name them from the SSoT. Stated here rather than "
                          "omitted, so a reader can tell &lsquo;not retained&rsquo; apart from &lsquo;nobody "
                          "approved&rsquo;. Fixing that is a change to the actor&rsquo;s SSoT semantics, not to "
                          "this demo.")))
              (table ["Op" "Unit" "Effect" "Approver at run time" "In the actor's record" "In the store" "In the ledger"]
                     (map attribution-row attribution)))

     (section "Social hand-off package"
              (str "<code>liftingequip.export/audit-package</code> and "
                   "<code>package-&gt;csv-bundle</code> materialize the body a manufacturer hands to conformity "
                   "or market-regulator inspectors. Counts are the export namespace&rsquo;s own, over the same "
                   "store this page rendered.")
              (str (table ["Artifact" "Count"]
                          (concat (map (fn [[k v]] (count-row (str "package :counts " k) v))
                                       (sort-by (comp str key) (:counts pkg)))
                                  (map (fn [[fname body]]
                                         (count-row (str "CSV " fname " (data rows)")
                                                    (max 0 (dec (count (str/split-lines body))))))
                                       (sort-by key bundle))))))

     "</main>\n"
     "<footer>\n"
     "  <p>Generated by <code>clojure -M:dev:render-html</code> "
     "(<code>liftingequip.render-html</code>, ISIC 2816, business id <code>" (esc (:business-id pkg)) "</code>). "
     "Deterministic: no timestamp, no clock read, no randomness &mdash; re-running against the same seed produces a "
     "byte-identical file. The generator throws instead of writing when the run yields no HARD governor hold.</p>\n"
     "</footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db journal] :as run} (run-demo!)
        hs (hard-holds db)]
    (when (empty? hs)
      (throw (ex-info "no governor hold fact on the ledger — refusing to write a console that shows no real hold"
                      {:ledger-facts (count (store/ledger db))})))
    (let [html (render run)]
      (spit out html)
      (println "wrote" out
               (str "(" (count html) " bytes, "
                    (count journal) " runs, "
                    (count (store/ledger db)) " ledger facts, "
                    (count hs) " HARD holds over "
                    (count (distinct (map #(-> % :violations first :rule) hs))) " distinct rules, "
                    (count (store/dispatch-history db)) " dispatch draft(s), "
                    (count (store/evidence-history db)) " certificate draft(s))")))))
