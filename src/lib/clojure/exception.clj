(ns lib.clojure.exception
  (:require [lib.clojure-string.core :as string]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-message-or-name
  "Returns the exception message or class name if the message is empty."
  [throwable]
  (or (-> (.getMessage ^Throwable throwable) (string/not-empty))
      (.getCanonicalName (class throwable))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-message-all
  "Builds single message from all nested exceptions.
   Includes optional `context` string as part of the message."
  ([throwable] (ex-message-all throwable nil))
  ([throwable, context]
   (when throwable
     (loop [sb (-> (StringBuilder.)
                   (cond-> context (-> (.append (str context))
                                       (.append " -> ")))
                   (.append (ex-message-or-name throwable)))
            cause (.getCause ^Throwable throwable)]
       (if cause
         (recur (-> sb (.append " -> ") (.append (ex-message-or-name cause)))
                (.getCause cause))
         (.toString sb))))))

(comment
  (def e (ex-info "One" {:x :one}
                  (ex-info "Two" {:x :two}
                           (ex-info "Three" {:x :three}))))
  (ex-message-all e)
  #_"One -> Two -> Three"
  (ex-message-all e "Prefix")
  #_"Prefix -> One -> Two -> Three")

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-root-cause
  "Find root cause for the throwable."
  [throwable]
  (if-let [cause (ex-cause throwable)]
    (recur cause)
    throwable))

(comment
  (ex-root-cause (ex-info "One" {:x :one}
                          (ex-info "Two" {:x :two}
                                   (ex-info "Three" {:x :three})))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
