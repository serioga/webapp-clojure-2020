(ns lib.integrant.async
  "Asynchronous utility for the integrant which allows to init/halt system keys
  in parallel."
  (:require [integrant.core :as ig]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- try-deref
  "Dereferences future/promise with 10 seconds timeout. Catches exceptions.
  Returns value of ref or nil on exception."
  [ref]
  (try (deref ref 10000 nil)
       (catch Throwable _)))

(defn reverse-run!
  "Applies a side-effectful function f to each key value pair in a system map
  asynchronously. Keys are traversed in reverse dependency order. The function
  f should take two arguments, a key and value."
  [system ks f]
  {:pre [(map? system) (some-> system meta ::ig/origin)]}
  (let [origin (#'ig/system-origin system)
        keys-promises (zipmap (#'ig/reverse-dependent-keys origin ks)
                              (repeatedly promise))]
    (->> keys-promises
         (map (fn [[k p]]
                (future
                  (try
                    (some->> (#'ig/reverse-dependent-keys origin (list k))
                             (remove (partial identical? k))
                             (seq)
                             (run! (comp try-deref keys-promises)))
                    (f k (system k))
                    (finally
                      (deliver p nil))))))
         (doall)
         (run! try-deref))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- build-key
  "Asynchronous version of the `integrant.core/build-key`."
  [f assertf resolvef system [k v]]
  (assoc system k (future
                    (let [v' (#'ig/expand-key system resolvef v)]
                      (assertf system k v')
                      {::value v' ::impl (f k v')}))))

(defn- build*
  "Copy of the `integrant.core/build` using modified `build-key` for
  asynchronous initialization."
  ([config, ks, f, assertf, resolvef]
   {:pre [(map? config)]}
   (let [relevant-keys (#'ig/dependent-keys config ks)
         relevant-config (select-keys config relevant-keys)]
     (when-let [invalid-key (first (#'ig/invalid-composite-keys config))]
       (throw (#'ig/invalid-composite-key-exception config invalid-key)))
     (when-let [ref (first (#'ig/ambiguous-refs relevant-config))]
       (throw (#'ig/ambiguous-key-exception config ref (map key (ig/find-derived config ref)))))
     (when-let [refs (seq (#'ig/missing-refs relevant-config))]
       (throw (#'ig/missing-refs-exception config refs)))
     (reduce (partial build-key f assertf resolvef)
             (with-meta {} {::ig/origin config})
             (map (fn [k] [k (config k)]) relevant-keys)))))

(defn build
  "Asynchronous version of the `integrant.core/build`."
  ([config, ks, f, assertf, resolvef]
   (let [system (build* config ks f assertf (comp ::impl deref resolvef))
         system (reduce (fn [system [k ref]]
                          (try
                            (let [{::keys [value impl]} (deref ref)]
                              (-> (assoc system k impl)
                                  (vary-meta assoc-in [::ig/build k] value)))
                            (catch Exception e
                              (vary-meta system assoc-in [::build-errors k] (ex-cause e)))))
                        (empty system)
                        system)]
     (when-let [errors (-> system meta ::build-errors)]
       (let [k (some (set (keys errors)) (#'ig/dependent-keys config ks))]
         (throw (#'ig/build-exception system f k nil (errors k)))))
     system)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn init
  "Turn a config map into an system map asynchronously. Keys are traversed in
  dependency order, initiated via the init-key multimethod, then the refs
  associated with the key are expanded. Every init-key is invoked in separate
  thread, first exception is raised."
  ([config]
   (init config (keys config)))
  ([config ks]
   {:pre [(map? config)]}
   (build config ks ig/init-key #'ig/assert-pre-init-spec ig/resolve-key)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn halt!
  "Halts a system map by applying halt-key! asynchronously in reverse dependency
  order. Every halt-key! is invoked in separate thread, exceptions are ignored."
  ([system]
   (halt! system (keys system)))
  ([system ks]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (reverse-run! system ks ig/halt-key!)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn resume
  "Turn a config map into a system map asynchronously, reusing resources from an
  existing system when it's possible to do so. Keys are traversed in dependency
  order, resumed with the resume-key multimethod, then the refs associated with
  the key are expanded. Every init-key is invoked in separate thread, first
  exception is raised. The halt-missing-keys! is invoked synchronously same like
  in the integrant.core/resume."
  ([config system]
   (resume config system (keys config)))
  ([config system ks]
   {:pre [(map? config) (map? system) (some-> system meta ::ig/origin)]}
   (#'ig/halt-missing-keys! config system ks)
   (build config ks (fn [k v]
                      (if (contains? system k)
                        (ig/resume-key k v (-> system meta ::ig/build (get k)) (system k))
                        (ig/init-key k v)))
          #'ig/assert-pre-init-spec
          ig/resolve-key)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suspend!
  "Suspends a system map by applying halt-key! asynchronously in reverse
  dependency order. Every suspend-key! is invoked in separate thread, exceptions
  are ignored."
  ([system]
   (suspend! system (keys system)))
  ([system ks]
   {:pre [(map? system) (some-> system meta ::ig/origin)]}
   (reverse-run! system ks ig/suspend-key!)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
