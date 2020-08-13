(ns dev.env.system.unit.tailwind
  (:require
    [clojure.java.shell :as shell]
    [clojure.string :as string]
    [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)


(def ^:private postcss-cmd
  (let [os-name (System/getProperty "os.name")]
    (if (string/includes? os-name "Windows")
      "node_modules/.bin/postcss.cmd"
      "node_modules/.bin/postcss")))


(defn- tailwind-shell-cmd
  [webapp]
  [postcss-cmd
   (str "tailwind/app/web_" webapp "/main.css")
   "-o" (str "resources/public/app/" webapp "/main.css")
   "--config" "dev/"])


(defn- build-webapp-css
  [{:keys [webapp on-rebuild]}]
  (let [cmd (tailwind-shell-cmd webapp)
        _ (log/info "Building webapp CSS..." webapp cmd)
        {:keys [out err]} (apply shell/sh cmd)]
    (if (empty? err)
      (do
        (log/info "[OK] Building webapp CSS" webapp out)
        (when on-rebuild
          (on-rebuild)))
      (log/error err))))


(defn watcher-handler
  "Watch handler for Tailwind CSS sources."
  [options]
  (fn [& _]
    (build-webapp-css options)))
