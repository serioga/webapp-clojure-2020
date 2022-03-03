(ns lib.util.uuid
  (:require [lib.clojure.core :as c])
  (:import (java.util UUID)))

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn from-string
  "Returns UUID for the string representation. Accepts only zero-padded
  representation. Returns `nil` for `nil`."
  [s]
  (when (some? s)
    (c/assert-pred s string? `from-string)
    (try (let [uuid (UUID/fromString s)]
           (when (= s (str uuid))
             uuid))
         (catch Throwable _))))

(comment
  (from-string "123")                                       ; Execution time mean : 4506,021 ns
  #_nil
  (from-string "49230eb0-9e0c-4d5e-b22e-3bd022cc72d0")      ; Execution time mean :  344,776 ns
  #_#uuid"49230eb0-9e0c-4d5e-b22e-3bd022cc72d0"
  (UUID/fromString "4-9-4-b-3")                             ; Execution time mean :  142,046 ns
  #_#uuid"00000004-0009-0004-000b-000000000003"
  (from-string "4-9-4-b-3")                                 ; Execution time mean :  191,114 ns
  #_nil
  (from-string nil)                                         ; Execution time mean :    1,315 ns
  #_nil
  (from-string [1 2 3])
  ;;clojure.lang.ExceptionInfo: lib.util.uuid/from-string - Assert failed: (assert-pred s string?)
  ;; #:lib.clojure.assert{:value [1 2 3], :type clojure.lang.PersistentVector, :failure :assertion-failed}
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
