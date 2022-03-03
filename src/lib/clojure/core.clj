(ns lib.clojure.core
  (:refer-clojure :exclude [assert, future])
  (:require [lib.clojure.assert]
            [lib.clojure.exception]
            [lib.clojure.future]
            [lib.clojure.lang]
            [lib.clojure.print]
            [medley.core]
            [potemkin :refer [import-vars]]))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(import-vars [lib.clojure.assert assert, assert-pred, assert-try]

             [lib.clojure.exception ex-message-all, ex-root-cause]

             [lib.clojure.future future]

             [lib.clojure.print pr-str*]

             [lib.clojure.lang add-method, first-arg, second-arg, invoke, select, unwrap-fn, unwrap-future])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;; The `declare` is a workaround for name resolution in Cursive.
;; See https://github.com/cursive-ide/cursive/issues/2411.

(declare find-first map-entry)
(import-vars [medley.core find-first map-entry])

(declare map-kv map-keys map-vals)
(import-vars [medley.core map-kv map-keys map-vals])

(declare filter-kv filter-keys filter-vals)
(import-vars [medley.core filter-kv filter-keys filter-vals])

(declare remove-kv remove-keys remove-vals)
(import-vars [medley.core remove-kv remove-keys remove-vals])

(declare deep-merge)
(import-vars [medley.core deep-merge])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
