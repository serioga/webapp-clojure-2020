(ns dev.env.system.app
  "Wrap app system with development related adjustments."
  (:require [app.system.core :as app]
            [clojure.string :as str]
            [dev.env.reload.ring-refresh :as ring-refresh]
            [me.raynes.fs :as fs]
            [ring.middleware.lint :as lint]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private user-props-file "dev/app/config/user.props")

(defn- prepare-prop-files
  [prop-files]
  (cond
    (not (fs/file? user-props-file)), prop-files
    (string? prop-files), (str/join "," [prop-files user-props-file])
    (sequential? prop-files), (-> (into [] prop-files)
                                  (conj user-props-file))
    :else user-props-file))

(comment
  (prepare-prop-files nil)
  (prepare-prop-files "dev/app/config/default.props")
  (prepare-prop-files ["dev/app/config/default.props"])
  (prepare-prop-files '("dev/app/config/default.props")))

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

(defn- prepare-system-config
  [config]
  (assoc config ::app/dev-mode true
                :dev.env.system/prepare-prop-files prepare-prop-files
                :dev.env.system/prepare-webapp prepare-webapp))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start
  "Start `app` system."
  ([]
   (start {}))
  ([{:keys [system-keys]}]
   (try (app/start (cond-> {:prepare-config prepare-system-config}
                     system-keys (assoc :system-keys system-keys)))
        (catch Throwable e
          (throw (->> e (Exception. (str 'app/start))))))
   (ring-refresh/send-refresh true)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop
  "Stop `app` system."
  []
  (ring-refresh/send-refresh false)
  (app/stop))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suspend
  "Suspend `app` system."
  []
  (ring-refresh/send-refresh false)
  (app/suspend))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn resume
  "Resume `app` system."
  []
  (try (app/resume {:prepare-config prepare-system-config})
       (catch Throwable e
         (throw (->> e (Exception. (str 'app/resume))))))
  (ring-refresh/send-refresh true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
