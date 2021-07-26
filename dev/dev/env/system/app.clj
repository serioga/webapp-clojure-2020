(ns dev.env.system.app
  "Wrap app system with development related adjustments."
  (:require [app.system.core :as app]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [dev.env.reload.ring-refresh :as ring-refresh]
            [lib.clojure.core :as e]
            [ring.middleware.lint :as lint])
  (:import (java.io File)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private user-props-file "dev/app/config/user.props")

(defn- prepare-prop-files
  [prop-files]
  (let [user-file (when (.exists ^File (io/as-file user-props-file)) user-props-file)]
    (cond
      (not user-file), prop-files
      (string? prop-files), (str/join "," [prop-files user-file])
      (sequential? prop-files), (-> (into [] prop-files)
                                    (conj user-file))
      :else user-file)))

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

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn start!
  "Start `app` system."
  ([]
   (start! {}))
  ([{:keys [system-keys]}]
   (e/try-wrap-ex 'app/start!
     (app/start! (cond-> {:prepare-config prepare-system-config}
                   system-keys (assoc :system-keys system-keys))))
   (ring-refresh/send-refresh! true)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn stop!
  "Stop `app` system."
  []
  (ring-refresh/send-refresh! false)
  (app/stop!))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suspend!
  "Suspend `app` system."
  []
  (ring-refresh/send-refresh! false)
  (app/suspend!))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn resume!
  "Resume `app` system."
  []
  (e/try-wrap-ex 'app/resume!
    (app/resume! {:prepare-config prepare-system-config}))
  (ring-refresh/send-refresh! true))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
