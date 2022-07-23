#!/usr/bin/env bb

; =============================================================================
; FEATUREFLAGS.CLJ
; USE: ./featureflags.clj --flag [flag-name] --geo [geo] --org [org-id]
; * Execute from base folder of a service, such as src/Services/Consent.
; * Will go through all appsettings of the service and calculate current value
; * of the given feature flag for the given org. Will also print calculation
; * traces.
; =============================================================================

(require '[clojure.tools.cli :refer [parse-opts]])
(use '[cheshire.core :as json])
(use '[clojure.java.io :only (file)])
(use `[clojure.string :only (replace) :as str])

(defn parse-args [args]
  (let [options
          [["-f" "--flag FEATURE-FLAG" "Name of the feature flag"]
          ["-g" "--geo GEO" "Geo of the organization"]
          ["-o" "--org ORGANIZATION-ID" "Id of the organization"]]
        args (:options (parse-opts args options))
       ]
    (do
      (assert (:flag args) "--flag must be specified")
      (assert (:geo args) "--geo must be specified")
      (assert (:org args) "--org must be specified")
      args)))

(def args (parse-args *command-line-args*))

(defn traverse [tree nodes]
  (if (empty? nodes)
    tree
    (let [[x & xs] nodes]
      (if (contains? tree x)
        (traverse (tree x) xs)
        nil))))

(defn parse-settings [file]
  (let [content (slurp (.getPath file))
        json (parse-string (str/replace content #" //(.*)" ""))]
    (traverse json ["FeatureFlags" (:flag args)])))

(defn load-settings []
  (let [service-dir (file "src/Service")
        service-files (.listFiles service-dir)
        deployment-dir (file "deployment/consent/appsettings")
        deployment-files (.listFiles deployment-dir)]
    (remove (fn [entry] (nil? (:content entry)))
      (map (fn [file] {:file file :content (parse-settings file)})
        (concat
          (filter #(= "appsettings.json" (.getName %)) service-files)
          (filter #(.contains (.getName %) (:geo args)) service-files)
          (filter #(= "appsettings.json" (.getName %)) deployment-files)
          (filter #(.contains (.getName %) (:geo args)) deployment-files))))))

(defn visit-settings [acc {file :file, content :content}]
  (let [filedescr (.getPath file)
        geovalue (content "value")
        orgvalue (traverse content ["overrides" (str "org|" (:org args))])]
    (when (some? geovalue) (println "> [GEO]" filedescr "=>" geovalue))
    (when (some? orgvalue) (println "> [ORG]" filedescr "=>" orgvalue))
    (if (some? orgvalue) orgvalue geovalue)))

(reduce visit-settings nil (load-settings))
