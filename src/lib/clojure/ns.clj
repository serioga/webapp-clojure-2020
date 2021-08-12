(ns lib.clojure.ns
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [lib.clojure.core :as e])
  (:import (java.io FilenameFilter)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- ns->path
  [s]
  (-> (name s)
      (string/replace "." "/")
      (string/replace "-" "_")))

(defn- filename->ns
  [s]
  (-> (re-find #"[^.]+" s)
      (string/replace "_" "-")))

(defn- list-dir-ns
  ([n] (list-dir-ns n :clj))
  ([n ext]
   (let [n (-> n name (string/replace #"\.[_*]?$" ""))
         ext (str "." (name ext))
         path (ns->path n)]
     (map (fn [filename]
            (symbol (str n "." (filename->ns filename))))
          (-> (or (io/resource path)
                  (throw (e/ex-info [" Folder not found " path])))
              (io/as-file)
              (.list (reify FilenameFilter
                       (accept [_ _ name] (string/ends-with? name ext)))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmacro require-dir
  "Loads all child namespaces in namespace `n` from files with extension `ext`.
   Trailing characters '_' or '*' of `n` are ignored.
   Extension `ext` is string or keyword, default extension is \"clj\".
   Examples:
     `(require-dir 'app.my-ns._)`
     `(require-dir 'app.my-ns.* :cljc)`"
  ([n] `(require-dir ~n :clj))
  ([n ext]
   `(require ~@(map (partial list `quote)
                    (list-dir-ns (eval n) ext)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
