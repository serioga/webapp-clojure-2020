(ns lib.clojure.exception)

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(def ^:const _->_
  "Error message separator for nested exceptions etc."
  "->")

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-message-or-name
  "Returns the message attached to ex if ex is a Throwable.
   If message is null then class name is used."
  [^Throwable ex]
  (or (.getMessage ex)
      (.getCanonicalName (class ex))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-message-all
  "Collect single message from all nested exceptions."
  [^Throwable ex]
  (when (instance? Throwable ex)
    (loop [message (ex-message-or-name ex)
           cause (ex-cause ex)]
      (if cause
        (recur (str message \space _->_ \space (ex-message-or-name cause))
               (ex-cause cause))
        message))))

(comment
  (ex-message-all (ex-info "One" {:x :one}
                           (ex-info "Two" {:x :two}
                                    (ex-info "Three" {:x :three}))))
  #_"One -> Two -> Three")

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn ex-root-cause
  "Find root cause for exception."
  [^Throwable ex]
  (if-let [cause (ex-cause ex)]
    (recur cause)
    ex))

(comment
  (ex-root-cause (ex-info "One" {:x :one}
                          (ex-info "Two" {:x :two}
                                   (ex-info "Three" {:x :three})))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
