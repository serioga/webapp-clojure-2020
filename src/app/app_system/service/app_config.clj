(ns app.app-system.service.app-config
  (:require
    [app.lib.config.props :as props]
    [app.lib.util.secret :as secret]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [integrant.core :as ig]))

(set! *warn-on-reflection* true)


(defmethod props/conform-prop-val :edn [_ v] (edn/read-string v))
(defmethod props/conform-prop-val :vector [_ v] (str/split v #"\s+"))
(defmethod props/conform-prop-val :set [_ v] (set (str/split v #"\s+")))
(defmethod props/conform-prop-val :secret [_ v] (secret/->Secret v))


(defmethod ig/init-key :app-system.service/app-config
  [_ {:keys [prop-files conform-rules prop-defaults :dev/prepare-prop-files]}]
  (let [prepare-prop-files (or prepare-prop-files identity)
        loaded (-> prop-files
                   (prepare-prop-files)
                   (props/load-prop-files))
        merged (merge prop-defaults
                      (-> loaded
                          (merge (System/getProperties))
                          (props/apply-conform-rules conform-rules)))]
    (with-meta merged (meta loaded))))


(comment
  (into (sorted-map)
        (ig/init-key :app-system.service/app-config
                     {:prop-files "dev-resources/dev/config/default.props"
                      :prop-defaults {"xxx" :xxx
                                      "Vk.App.Id" nil}
                      :conform-rules {"Mailer.Smtp.Port" :edn
                                      "Mailer.Smtp.Options" :edn
                                      #"System\.Switch\..+" :edn
                                      #"Webapp\.Hosts\(.+\)" :set}})))

(comment
  (System/getProperties)
  (System/getenv))
