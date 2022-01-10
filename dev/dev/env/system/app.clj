(ns dev.env.system.app
  "Wrap app system with development related adjustments."
  (:require [app.system.core :as app.system]
            [clojure.string :as string]
            [dev.env.reload.ring-refresh :as ring-refresh]
            [me.raynes.fs :as fs]
            [ring.middleware.lint :as lint]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(derive :app.system.service/hikari-data-source, :lib.integrant.system/keep-running-on-suspend)
(derive :app.system.task/update-database-schema :lib.integrant.system/keep-running-on-suspend)
(derive :lib.integrant.system/import-map,,,,,,, :lib.integrant.system/keep-running-on-suspend)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private user-props-file "dev/app/config/user.props")

(defn- prepare-prop-files
  [prop-files]
  (cond
    (not (fs/file? user-props-file)), prop-files
    (string? prop-files), (string/join "," [prop-files user-props-file])
    (sequential? prop-files), (-> (into [] prop-files)
                                  (conj user-props-file))
    :else user-props-file))

(comment
  (prepare-prop-files nil)
  (prepare-prop-files "dev/app/config/default.props")
  (prepare-prop-files ["dev/app/config/default.props"])
  (prepare-prop-files '("dev/app/config/default.props")))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- wrap-webapp-handler
  [_webapp]
  (fn [handler]
    (-> handler
        lint/wrap-lint
        ring-refresh/wrap-refresh)))

(defn- prepare-webapp
  [webapp]
  (-> webapp
      (update :handler (wrap-webapp-handler webapp))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private changelog-track-dir "resources/app/database/schema")

(defn- dir-mod-time
  [dir]
  (->> (fs/iterate-dir dir)
       (map (comp fs/mod-time first))
       (reduce max 0)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- prepare-system-config
  [config]
  (assoc config :app.system.config/dev-mode true
                :dev.env.system/prepare-prop-files prepare-prop-files
                :dev.env.system/prepare-webapp prepare-webapp
                :dev.env.system/db-changelog-mod-time (dir-mod-time changelog-track-dir)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start
  "Start `app` system."
  ([]
   (start {}))
  ([{:keys [system-keys]}]
   (try (app.system/start (cond-> {:prepare-config prepare-system-config}
                            system-keys (assoc :system-keys system-keys)))
        (catch Throwable e
          (throw (->> e (Exception. (str 'app.system/start))))))
   (ring-refresh/send-refresh true)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop
  "Stop `app` system."
  []
  (ring-refresh/send-refresh false)
  (app.system/stop))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suspend
  "Suspend `app` system."
  []
  (ring-refresh/send-refresh false)
  (app.system/suspend))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn resume
  "Resume `app` system."
  []
  (try (app.system/resume {:prepare-config prepare-system-config})
       (catch Throwable e
         (throw (->> e (Exception. (str 'app.system/resume))))))
  (ring-refresh/send-refresh true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
