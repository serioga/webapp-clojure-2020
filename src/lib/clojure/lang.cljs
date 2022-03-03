(ns lib.clojure.lang)

(set! *warn-on-infer* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn add-method
  "Installs a method of multimethod associated with dispatch-value."
  [multi-fn dispatch-val method]
  (-add-method ^cljs.core/MultiFn multi-fn dispatch-val method))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn first-arg
  "Returns first argument. To be used as multimethod dispatch function."
  ([a] a)
  ([a _] a)
  ([a _ _] a)
  ([a _ _ & _] a))

(defn second-arg
  "Returns second argument. To be used as multimethod dispatch function."
  ([_ a] a)
  ([_ a _] a)
  ([_ a _ & _] a))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn asserted
  "Returns `x` if `(pred x)` is logical true, else `nil`.
   Returns #(asserted % pred) in case of 1-arity."
  ([pred]
   #(asserted % pred))
  ([x pred]
   (when (pred x) x)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

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

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
