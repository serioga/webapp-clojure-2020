(ns lib.clojure.print)

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(deftype StringLiteral [x])

(defmethod print-method StringLiteral [o, w]
  (binding [*print-readably* nil]
    (print-method (.x ^StringLiteral o) w)))

(defmacro p-str
  "Prints to string like `clojure.core/pr-str` but string literals without quotes."
  [& more]
  `(pr-str ~@(->> more (map #(if (string? %) (list ->StringLiteral %), %)))))

(comment
  (macroexpand-1 '(p-str "a" "b" "c" 'd (str "e") {:f "f"} "" (str "")))
  (p-str "a" "b" "c" 'd (str "e") {:f "f"} nil (str ""))
  #_"a b c d \"e\" {:f \"f\"} nil \"\""
  (println (p-str "a" "b" "c" 'd (str "e") {:f "f"} nil (str "")))
  ;;a b c d "e" {:f "f"} nil ""
  )

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
