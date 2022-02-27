(ns dev.env.tailwind.watcher
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]
            [lib.clojure-tools-logging.logger :as logger]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:private logger (logger/get-logger *ns*))

(def ^:private postcss-cmd
  (let [os-name (System/getProperty "os.name")]
    (if (string/includes? os-name "Windows")
      "node_modules/.bin/postcss.cmd"
      "node_modules/.bin/postcss")))

(defn- tailwind-shell-cmd
  [webapp]
  [postcss-cmd
   (str "tailwind/app/$_" webapp "/main.css")
   "-o" (str "resources/public/app/" webapp "/main.css")
   "--config" "dev/app/config/"])

(defn- build-webapp-css
  [{:keys [webapp on-rebuild]}]
  (let [cmd (tailwind-shell-cmd webapp)
        _ (logger/info logger (print-str "Building webapp CSS..." webapp cmd))
        {:keys [out err]} (apply shell/sh cmd)]
    (if (empty? err)
      (do (logger/info logger (print-str "[OK] Building webapp CSS" webapp out))
          (when on-rebuild (on-rebuild)))
      (logger/error logger err))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn watch-handler
  "Watch handler for Tailwind CSS sources."
  [options]
  (fn [& _]
    (build-webapp-css options)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
