(ns dev.dev-system.app-system
  "Wrap app-system with development related adjustments."
  (:require
    [app.app-system.core :as app-system]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [ring.middleware.lint :as lint])
  (:import
    (java.io File)))

(set! *warn-on-reflection* true)


(defn prepare-prop-files
  [prop-files]
  (let [user (System/getProperty "user.name")
        user-file (str "dev-resources/dev/config/user." user ".props")
        user-file-exists? (.exists ^File (io/as-file user-file))]
    (cond
      (not user-file-exists?), prop-files
      (string? prop-files), (str/join "," [prop-files user-file])
      (sequential? prop-files), (-> (into [] prop-files)
                                  (conj user-file))
      :else user-file)))

#_(comment
    (prepare-prop-files nil)
    (prepare-prop-files "dev-resources/dev/config/default.props")
    (prepare-prop-files ["dev-resources/dev/config/default.props"])
    (prepare-prop-files '("dev-resources/dev/config/default.props")))


(defn wrap-webapp-handler
  [_webapp]
  (fn [handler]
    (-> handler
      lint/wrap-lint)))


(defn prepare-webapp
  [webapp]
  (-> webapp
    (update :handler (wrap-webapp-handler webapp))))


(defn prepare-system-config
  [config]
  (assoc config
    :app-system/dev-mode? true
    :app-system.dev/prepare-prop-files prepare-prop-files
    :app-system.dev/prepare-webapp prepare-webapp))


(defn start!
  ([]
   (start! {}))
  ([{:keys [system-keys]}]
   (app-system/start! (cond-> {:prepare-config prepare-system-config}
                        system-keys (assoc :system-keys system-keys)))))


(defn stop!
  []
  (app-system/stop!))


(defn suspend!
  []
  (app-system/suspend!))


(defn resume!
  []
  (app-system/resume!
    {:prepare-config prepare-system-config}))


#_(comment
    (time (keys (start!)))
    (time (suspend!))
    (time (keys (resume!)))
    (time (do
            (suspend!)
            (keys (resume!))))
    (time (stop!))

    (time (meta @(:app-system.service/*immutant-web
                   (start! {:system-keys [:app-system.service/*immutant-web]})))))
