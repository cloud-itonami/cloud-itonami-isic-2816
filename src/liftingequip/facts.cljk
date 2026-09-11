(ns liftingequip.facts
  "Per-jurisdiction lifting-equipment design/proof-load-test-conformity
  catalog -- the G2-style spec-basis table the Lifting Equipment
  Governor checks every `:design-rules/verify` proposal against.

  Coverage is reported HONESTLY: a jurisdiction not in this table has
  NO spec-basis. Seed values cite official lifting-equipment safety /
  conformity authorities: the ASME B30 series (Safety Standards for
  Cableways, Cranes, Derricks, Hoists, Hooks, Jacks, and Slings) + OSHA
  29 CFR 1926.1400 (Cranes and Derricks in Construction) for USA, the
  EU Machinery Directive 2006/42/EC (CE marking) + EN 13001 (crane
  safety, general design) for DEU, JIS B 8821/B 8830 + the Industrial
  Safety and Health Act (労働安全衛生法) crane-related regulations
  (クレーン等安全規則) for JPN, and the Lifting Operations and Lifting
  Equipment Regulations 1998 (LOLER) + BS EN 13001 (UKCA-adopted) for
  GBR -- this is a starting catalog, not a survey of every market.")

(def catalog
  {"JPN" {:name "Japan"
          :owner-authority "厚生労働省 (MHLW) / 経済産業省 (METI) / 日本産業規格 (JIS)"
          :legal-basis "労働安全衛生法 (クレーン等安全規則) / JIS B 8821・B 8830 (荷重試験、ASME B30/EN 13001 参考整合)"
          :national-spec "クレーン・ホイスト・フォークリフト等の荷重試験(定格荷重の1.25倍等)および設計適合要件"
          :provenance "https://www.mhlw.go.jp/"
          :required-evidence ["荷重試験報告書 (load-test-report)"
                              "材料証明記録 (material-certification-record)"
                              "溶接施工記録 (weld-procedure-qualification-record)"
                              "設計計算適合記録 (design-calculation-conformity-record)"]}
   "USA" {:name "United States"
          :owner-authority "ASME (B30 Committee) / OSHA"
          :legal-basis "ASME B30 series (B30.2 Overhead & Gantry Cranes, B30.5 Mobile & Locomotive Cranes, B30.9 Slings, B30.10 Hooks, B30.16 Overhead Hoists, B30.22 Articulating Boom Cranes) / OSHA 29 CFR 1926.1400 (Cranes and Derricks in Construction, proof-load test requirement)"
          :national-spec "US crane, hoist and forklift design, fabrication and proof-load-test acceptance requirements"
          :provenance "https://www.asme.org/codes-standards/find-codes-standards/b30-safety-standard-cableways-cranes-derricks-hoists-hooks-jacks-slings"
          :required-evidence ["load-test-report"
                              "material-certification-record"
                              "weld-procedure-qualification-record"
                              "design-calculation-conformity-record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "HSE / UK lifting-equipment framework"
          :legal-basis "Lifting Operations and Lifting Equipment Regulations 1998 (LOLER) / BS EN 13001 (crane safety, UKCA-adopted, reference)"
          :national-spec "UK lifting-equipment thorough-examination and proof-load-test conformity requirements"
          :provenance "https://www.hse.gov.uk/work-equipment-machinery/loler.htm"
          :required-evidence ["load-test-report"
                              "material-certification-record"
                              "weld-procedure-qualification-record"
                              "design-calculation-conformity-record"]}
   "DEU" {:name "Germany"
          :owner-authority "notifizierte Stelle (CE-Kennzeichnung) / DIN / EU-Maschinenrichtlinie-Kontext"
          :legal-basis "Maschinenrichtlinie 2006/42/EG (Machinery Directive) / DIN EN 13001 (Kransicherheit, Konstruktion allgemein, Referenz)"
          :national-spec "DE/EU Kran-Konformitätsbewertung und Lastprüfungsanforderungen"
          :provenance "https://www.din.de/"
          :required-evidence ["Lastprüfbericht (load-test-report)"
                              "Werkstoffzertifikat (material-certification-record)"
                              "Schweißverfahrensprüfung (weld-procedure-qualification-record)"
                              "Festigkeitsberechnungsnachweis (design-calculation-conformity-record)"]}})

(defn spec-basis [iso3] (get catalog iso3))

(defn coverage
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-2816 R0: " (count catalog)
                 " jurisdictions seeded. Extend `liftingequip.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
