(ns lib.config.props
  "Application configuration."
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojurewerkz.propertied.properties :as p]
            [lib.clojure.core :as e])
  (:import (java.util.regex Pattern)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- load-map-from-props-file
  "Load props from single file as map."
  [file]
  (some-> file
          (io/file)
          (p/load-from)
          (p/properties->map)))

(defn- string->filenames
  "Split comma separated file names to list."
  [s]
  (string/split s #","))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn load-prop-files
  "Load props files by sequence of filenames. Return combined hash-map."
  [filenames]
  (let [filenames (cond-> filenames
                    (string? filenames) string->filenames)]
    (e/assert filenames sequential?)
    (with-meta (->> filenames
                    (map load-map-from-props-file)
                    (reduce merge {}))
               {:prop-files filenames})))

(comment
  (meta (load-prop-files "dev/app/config/default.props"))
  (meta (load-prop-files ["dev/app/config/default.props"]))
  (meta (load-prop-files (list "dev/app/config/default.props")))
  (meta (load-prop-files ["dev/app/config/default.props"
                          "dev/app/config/default.props"]))
  (meta (load-prop-files nil))
  (meta (merge (load-prop-files (list "dev/app/config/default.props"))
               (System/getProperties)))
  (re-matches #"Webapp\.Hosts\(.+\)" "Webapp.Hosts(ok)")
  (re-matches #"System\.Switch\..+" "System.Switch.BackendService")
  (keyword "Webapp.Hosts(ok)")
  (require '[integrant.core :as ig])
  (into (sorted-map)
        (ig/init-key :app.system.service/app-config
                     {:prop-files "dev/app/config/default.props"
                      :conform-rules {"Mailer.Smtp.Port" :edn
                                      "Mailer.Smtp.Options" :edn
                                      #"System\.Switch\..+" :edn
                                      #"Webapp\.Hosts\(.+\)" :set}})))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti ^:private conform-prop-val
  "Define conform rule by rule keyword."
  (fn [rule _value] rule))

(defn- conform-prop-val*
  [k rule value]
  (e/try-wrap-ex [['conform-prop-val k rule value]]
    (conform-prop-val rule value)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn add-conform-rule
  "Installs function f as a handler for the rule keyword in the `conform-prop-val`."
  [rule, f]
  (e/add-method conform-prop-val rule #(f %2)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn apply-conform-rules
  "Apply conform rules to props map values."
  [m rules]
  (let [apply-regex (fn [pattern rule]
                      (fn [cm mk v]
                        (if (re-matches pattern mk)
                          (assoc! cm mk (conform-prop-val* pattern rule v))
                          cm)))
        conformed
        (persistent! (reduce-kv (fn [cm k rule]
                                  (cond
                                    (instance? Pattern k), (reduce-kv (apply-regex k rule), cm, m)
                                    :else (if-some [v (m k)]
                                            (assoc! cm k (conform-prop-val* k rule v))
                                            cm)))
                                (transient {})
                                rules))]
    (merge m, conformed)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
