(ns app.rum.mixin.static-args)

#?(:clj  (set! *warn-on-reflection* true)
   :cljs (set! *warn-on-infer* true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn static-args-mixin
  "Avoid re-render if specific component’s arguments have not changed."
  [get-arg]
  {:should-update
   (fn [old-state new-state]
     (not= (get-arg (:rum/args old-state))
           (get-arg (:rum/args new-state))))})

(def static-first-arg-mixin
  "Avoid re-render if first component’s arguments have not changed."
  (static-args-mixin first))

(def static-second-arg-mixin
  "Avoid re-render if second component’s arguments have not changed."
  (static-args-mixin second))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
