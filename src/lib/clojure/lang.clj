(ns lib.clojure.lang)

(set! *warn-on-reflection* true)

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

(defn test-pred
  "Return `x` when `(pred x)` is truthy."
  [x pred]
  (when (pred x) x))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn opt-fn
  "If `v` is a function then invoke it else keep `v` as is.
   Useful to represent function parameters as value or function without arguments."
  [v]
  (if (fn? v) (v) v))

(comment
  (opt-fn 1)
  (opt-fn (constantly 1))
  (opt-fn nil)
  (opt-fn :kw))

;•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
