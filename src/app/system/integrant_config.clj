(ns app.system.integrant-config
  "Utility to build integrant config from templates."
  (:require [clojure.test :as test]
            [integrant.core :as ig]
            [lib.clojure.core :as c]
            [lib.integrant.system :as ig.system]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn suffix-key
  "Adds 'extension' to keyword `key`."
  [k ext]
  (let [k (ig.system/simple-key k)]
    (keyword (namespace k) (str (name k) ext))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti builder-mixin
  "Returns customized builder params."
  {:arglists '([id {:builder/keys [config-map config-key params] :as builder}])}
  (fn [id _] id))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti ^:private build-param
  "Returns transformed builder or nil if builder does not change."
  {:arglists '([id {:builder/keys [config-map config-key params] :as builder}])}
  (fn [id _] id))

(defn- reduce-builder-params
  [builder ks]
  (reduce (fn [builder id]
            (or (build-param id builder) builder))
          builder ks))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmethod build-param ::mixins
  [_ {:builder/keys [params] :as builder}]
  (when-let [mixins (::mixins params)]
    (c/assert-pred mixins sequential? :mixins)
    (->> mixins (reduce (fn [builder mixin]
                          (assoc builder :builder/params (builder-mixin mixin builder)))
                        builder))))

(defmethod build-param ::mounts
  [_ {:builder/keys [config-key params] :as builder}]
  (when-let [mounts (::mounts params)]
    (c/assert-pred mounts coll? ::mounts)
    (c/assert-pred mounts (partial every? keyword?) ::mounts)
    (when (seq mounts)
      (reduce (fn [builder mount-key]
                (update-in builder [:builder/config-map :app.system.service/mount]
                           assoc mount-key (ig/ref config-key)))
              builder mounts))))

(defmethod build-param ::awaits
  [_ {:builder/keys [params] :as builder}]
  (when-let [awaits (::awaits params)]
    (c/assert-pred awaits coll? ::awaits)
    (update-in builder [:builder/params ::config ::await-refs]
               merge (->> awaits (into {} (map (juxt identity ig/ref)))))))

(defmethod build-param ::derive
  [_ {:builder/keys [config-key params] :as builder}]
  (when-let [parent (::derive params)]
    (c/assert-pred parent keyword? ::derive)
    (c/assert-pred config-key keyword? "Cannot apply :derive to composite key")
    (assoc builder :builder/config-key [parent config-key])))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defmulti setup-builder
  "Returns config map for the builder. Used to implement custom setup when
  multiple config keys should be created in config map."
  ::setup)

(declare build-config)

(defmethod setup-builder nil
  [builder]
  (let [{:builder/keys [config-map config-key params]} (->> [::mixins ::mounts ::awaits ::derive]
                                                            (reduce-builder-params builder))]
    (if-let [import-keys (::import params)]
      (let [other-key (when (or (not (keyword? config-key))
                                (ig.system/get-key-method ig/init-key config-key))
                        (suffix-key (ig.system/simple-key config-key) ".config"))
            template {::derive ::ig.system/import-map
                      ::config {:init-map (or (::config params) {})
                                :import-from (ig/ref :app.system.service/app-config)
                                :import-keys import-keys}}]
        (build-config config-map (if other-key
                                   {config-key (ig/ref other-key), other-key template}
                                   {config-key template})))
      (assoc config-map config-key (::config params)))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn- builder-params?
  "Returns true if value is the map containing builder params."
  [value]
  (and (map? value) (->> (keys value) (some #(= (namespace ::_) (namespace %))))))

(defn- init-builder
  "Initializes builder record. Expects `params` to be builder params map.
  The optional ::setup is moving from params to the builder and defines how the
  builder will be installed.

  Builder record keys:
  - :builder/config-map Accumulating integrant config map.
  - :builder/config-key Current integrant key the config map template.
  - :builder/params     Builder params which define builder transformations.
  - ::setup             The optional custom setup ID.
  "
  [config-map config-key params]
  #:builder{:config-map config-map
            :config-key config-key
            :params (dissoc params ::setup)
            ::setup (::setup params)})

(defn- setup-template-entry
  [config-map [config-key value]]
  (if (builder-params? value)
    (-> (init-builder config-map config-key value)
        (setup-builder))
    (assoc config-map config-key value)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn build-config
  "Returns integrant config map built over all keys in the config map template.
  Accepts optional initial config map as first argument to append built keys.

  Template entry value can contain builder params:
  - ::mixins Vector of dispatch values for the builder-mixin multimethod.
  - ::config Initial data for config map value.
  - ::setup  Optional ID of customized setup. All params below relate to default
             setup only.
  - ::mounts Collection of keys to refer this config key in the
             :app.system.service/mount.
  - ::awaits Collection of keys to be referred in installed config value.
  - ::derive The key to derive installed config key from.
  - ::import Defines config value built from configuration properties.
  "
  ([template] (build-config {} template))
  ([config template]
   (reduce setup-template-entry config template)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/deftest build-config-test
  (defmethod builder-mixin :test/mixin
    [_ {:builder/keys [params]}]
    (-> params
        (c/deep-merge #::{:config {:test/mixin-value true}})
        (update ::awaits conj :test/mixin-await-key)))

  (test/testing "Builder ignores values without builder params."
    (test/are [arg ret] (= ret (build-config arg))
      #_arg {:test/key true},,, #_ret {:test/key true}
      #_arg {:test/key {:a 1}}, #_ret {:test/key {:a 1}}
      #_arg {:test/key [1 2 3]} #_ret {:test/key [1 2 3]}))

  (test/testing "Builder parameter: config."
    (test/are [arg ret] (= ret (build-config arg))
      #_arg {:test/key #::{:config {:test/value true}}}
      #_ret {:test/key {:test/value true}}))

  (test/testing "Builder parameter: mixins."
    (test/are [arg ret] (= ret (build-config arg))
      #_arg {:test/key #::{:mixins [:test/mixin]}}
      #_ret {:test/key {:test/mixin-value true,
                        :app.system.integrant-config/await-refs {:test/mixin-await-key #integrant.core.Ref{:key :test/mixin-await-key}}}}))

  (test/testing "Builder parameter: mounts."
    (test/are [arg ret] (= ret (build-config arg))
      #_arg {:test/key #::{:mounts #{:test/mount-key},}}
      #_ret {:app.system.service/mount {:test/mount-key #integrant.core.Ref{:key :test/key},},
             :test/key nil}))

  (test/testing "Builder parameter: awaits."
    (test/are [arg ret] (= ret (build-config arg))
      #_arg {:test/key #::{:awaits #{:test/await-key},}}
      #_ret {:test/key {:app.system.integrant-config/await-refs {:test/await-key #integrant.core.Ref{:key :test/await-key}}}}))

  (test/testing "Builder parameter: derive."
    (test/are [arg ret] (= ret (build-config arg))
      #_arg {:test/key #::{:derive :test/parent-key}}
      #_ret {[:test/parent-key :test/key] nil}))

  (test/testing "Builder parameter: import."
    (test/are [arg ret] (= ret (build-config arg))
      #_arg {:test/key #::{:config {:test/value true}
                           :import {:test/value "TestValue"}}}
      #_ret {[:lib.integrant.system/import-map :test/key] {:init-map {:test/value true},
                                                           :import-from #integrant.core.Ref{:key :app.system.service/app-config},
                                                           :import-keys {:test/value "TestValue"}}}
      #_arg {:test/key #::{:derive :test/parent-key
                           :config {:test/value true}
                           :import {:test/value "TestValue"}}}
      #_ret {[:test/parent-key :test/key] #integrant.core.Ref{:key :test/key.config},
             [:lib.integrant.system/import-map :test/key.config] {:init-map {:test/value true},
                                                                  :import-from #integrant.core.Ref{:key :app.system.service/app-config},
                                                                  :import-keys {:test/value "TestValue"}}}))

  (defmethod setup-builder :test/setup
    [{:builder/keys [config-map config-key params]}]
    (build-config config-map {config-key params
                              :test/setup-key (ig/ref config-key)}))

  (test/testing "Builder parameter: setup."
    (test/are [arg ret] (= ret (build-config arg))
      #_arg {:test/key #::{:setup :test/setup
                           :config {:test/value true}}}
      #_ret {:test/key {:test/value true}
             :test/setup-key #integrant.core.Ref{:key :test/key}})))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
