(ns app.lib.config.props
  "Application configuration."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [clojurewerkz.propertied.properties :as p]
    [taoensso.truss :as truss])
  (:import
    (java.util.regex Pattern)))

(set! *warn-on-reflection* true)


(defn ^:private load-map-from-props-file
  "Load props from single file as map."
  [file]
  (some-> file
    (io/file)
    (p/load-from)
    (p/properties->map)))


(defn ^:private string->filenames
  "Split comma separated file names to list."
  [s]
  (str/split s #","))


(defn load-prop-files
  "Load and merge properties from multiple files to single hash-map."
  [filenames]
  (let [filenames (cond-> filenames
                    (string? filenames) string->filenames)]
    (truss/have! sequential? filenames)
    (log/debug "Load properties from" (pr-str filenames))
    (with-meta
      (->> filenames
        (map load-map-from-props-file)
        (reduce merge {}))
      {:prop-files filenames})))

#_(comment
    (meta (load-prop-files "dev-resources/dev/config/default.props"))
    (meta (load-prop-files ["dev-resources/dev/config/default.props"]))
    (meta (load-prop-files (list "dev-resources/dev/config/default.props")))
    (meta (load-prop-files ["dev-resources/dev/config/default.props"
                            "dev-resources/dev/config/default.props"]))
    (meta (load-prop-files nil))
    (meta (merge
            (load-prop-files (list "dev-resources/dev/config/default.props"))
            (System/getProperties)))
    (re-matches #"Webapp\.Hosts\(.+\)" "Webapp.Hosts(ok)")
    (re-matches #"System\.Switch\..+" "System.Switch.BackendService")
    (keyword "Webapp.Hosts(ok)")
    (into (sorted-map)
      (ig/init-key :app-system.service/app-config
        {:prop-files "dev-resources/dev/config/default.props"
         :conform-rules {"Mailer.Smtp.Port" :edn
                         "Mailer.Smtp.Options" :edn
                         #"System\.Switch\..+" :edn
                         #"Webapp\.Hosts\(.+\)" :set}})))


(defmulti conform-prop-val
  "Apply conformance rule to property value."
  (fn [rule _value] rule))


(defn apply-conform-rules
  "Apply conformance rules to values in configuration map."
  [m rules]
  (let [apply-regex (fn [pattern rule]
                      (fn [cm mk v]
                        (if (re-matches pattern mk)
                          (assoc! cm mk (conform-prop-val rule v))
                          cm)))
        conformed (persistent!
                    (reduce-kv
                      (fn [cm k rule]
                        (cond
                          (instance? Pattern k), (reduce-kv
                                                   (apply-regex k rule), cm, m)
                          :else (if-some [v (m k)]
                                  (assoc! cm k (conform-prop-val rule v))
                                  cm)))
                      (transient {})
                      rules))]
    (merge m, conformed)))
