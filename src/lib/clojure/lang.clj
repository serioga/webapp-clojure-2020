(ns lib.clojure.lang
  (:import (clojure.lang MultiFn)
           (java.util.concurrent Future)))

(set! *warn-on-reflection* true)

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn add-method
  "Installs a method of multimethod associated with dispatch-value."
  [multi-fn dispatch-val method]
  (.addMethod ^MultiFn multi-fn dispatch-val method))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn invoke
  "Invokes function `f` with arguments.
   Performance implications for 5+ arguments."
  ([f] (f))
  ([f a] (f a))
  ([f a b] (f a b))
  ([f a b c] (f a b c))
  ([f a b c d] (f a b c d))
  ([f a b c d e] (f a b c d e))
  ([f a b c d e & args] (apply f a b c d e args)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn asserted
  "Returns `x` if `(pred x)` is logical true, else `nil`.
   Returns #(asserted % pred) in case of 1-arity."
  ([pred]
   #(asserted % pred))
  ([x pred]
   (when (pred x) x)))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn unwrap-fn
  "If `x` is a function, returns `(x)`, else returns `x`.
   Useful to represent function parameters as value or function without arguments."
  [x]
  (if (fn? x) (x) x))

(comment
  (unwrap-fn 1)
  (unwrap-fn (constantly 1))
  (unwrap-fn nil)
  (unwrap-fn :kw))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn unwrap-future
  "If `x` is future?, returns `(deref x)`, else returns `x`."
  [x]
  (if (future? x) (.get ^Future x), x))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
