(ns lib.clojure-string.core
  "Extension of `clojure.string`. Similar to cuerdas, superstring etc."
  (:refer-clojure :exclude [concat empty? not-empty replace])
  (:require [clojure.string :as string]
            [clojure.test :as test]
            [potemkin :refer [import-vars]])
  (:import (org.apache.commons.lang3 StringUtils)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(import-vars [clojure.string blank?, starts-with?, ends-with?, includes?]
             [clojure.string replace, replace-first, re-quote-replacement]
             [clojure.string capitalize, upper-case, lower-case]
             [clojure.string join, split, split-lines]
             [clojure.string trim, triml, trimr, trim-newline]
             [clojure.string index-of last-index-of])

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn empty?
  "True if string `s` is nil or has zero length."
  {:test #(test/are [form] form
            (true?, (empty? nil))
            (true?, (empty? ""))
            (false? (empty? "-")))}
  [s]
  (StringUtils/isEmpty s))

(defn not-empty
  "If `s` is empty, returns nil, else `s`."
  {:test #(test/are [form] form
            (nil?, (not-empty nil))
            (nil?, (not-empty ""))
            (= "-" (not-empty "-")))}
  [s]
  (when-not (empty? s) s))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn concat
  "Concatenates strings using native `.concat`.
   Works with strings only."
  {:tag String
   :test #(test/are [form] form
            (= "0123456789ABCDE" (concat "0123456789" "ABCDE"))
            (= "0123456789",,,,, (concat "0123456789" nil))
            (= "ABCDE",,,,,,,,,, (concat nil,,,,,,,,, "ABCDE"))
            (= "ABCDE",,,,,,,,,, (concat "A",,,,,,,,, "B" "C" "D" "E"))
            (nil?,,,,,,,,,,,,,,, (concat nil,,,,,,,,, nil)))}
  ([a b]
   (if a (if b (.concat ^String a b), a)
         (or b nil)))
  ([a b c]
   (-> a (concat b) (concat c)))
  ([a b c d]
   (-> a (concat b) (concat c) (concat d)))
  ([a b c d e]
   (-> a (concat b) (concat c) (concat d) (concat e))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn join-not-empty
  "Similar to `clojure.core/join` but skipping elements which produce empty output."
  [sep coll]
  (string/join sep (keep (comp not-empty str) coll)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn char-digit?
  "True if char `c` is digit."
  [c]
  (Character/isDigit ^Character c))

(defn char-whitespace?
  "True if `c` is whitespace character."
  [c]
  (Character/isWhitespace ^Character c))

(defn char-not=
  "Builds predicate for char non-equality."
  [c]
  #(if (.equals ^Character c %) false true))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn only-chars?
  "True if `s` contains only chars satisfying `pred`. False when `s` is empty."
  {:test #(test/are [form] form
            (false? (only-chars? nil,, char-digit?))
            (false? (only-chars? "",,, char-digit?))
            (false? (only-chars? "---" char-digit?))
            (true?, (only-chars? "123" char-digit?)))}
  [^CharSequence s, pred]
  (if (empty? s)
    false
    (let [len (.length s)]
      (loop [index 0]
        (if (= len index)
          true
          (if (pred (.charAt s (unchecked-int index)))
            (recur (unchecked-inc index))
            false))))))

(defn numeric?
  "True if `s` contains only digits."
  {:test #(test/are [form] form
            (true?, (numeric? "1234567890"))
            (false? (numeric? "1234567890x"))
            (false? (numeric? nil))
            (false? (numeric? "")))}
  [s]
  (StringUtils/isNumeric s))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn strip-start
  "Strips any of a set of characters from the start of a String.
   A `nil` input String returns `nil`.
   An empty string (\"\") input returns the empty string.
   Strips whitespaces if the string `strip-chars` is not specified."
  {:tag String
   :test #(test/are [form] form
            (= "test" (strip-start "test",,, " "))
            (= "test" (strip-start "   test" " "))
            (= "te  " (strip-start "   te  " " "))
            (= "test" (strip-start "   test"))
            (= "test" (strip-start "///test" "/"))
            (= "test" (strip-start "☺☺☺test" "☺"))
            (= "test" (strip-start "/?☺test" "/?☺"))
            (nil?,,,, (strip-start nil "/")))}
  ([s]
   (StringUtils/stripStart s nil))
  ([s, strip-chars]
   (StringUtils/stripStart s strip-chars)))

(defn strip-end
  "Strips any of a set of characters from the end of a String.
   A `nil` input String returns `nil`.
   An empty string (\"\") input returns the empty string.
   Strips whitespaces if the string `strip-chars` is not specified."
  {:tag String
   :test #(test/are [form] form
            (= "test" (strip-end "test",,, " "))
            (= "test" (strip-end "test   " " "))
            (= "  st" (strip-end "  st   " " "))
            (= "test" (strip-end "test   "))
            (= "test" (strip-end "test///" "/"))
            (= "test" (strip-end "test☺☺☺" "☺"))
            (= "test" (strip-end "test/?☺" "/?☺"))
            (nil?,,,, (strip-end nil "/")))}
  ([s]
   (StringUtils/stripEnd s nil))
  ([s, strip-chars]
   (StringUtils/stripEnd s strip-chars)))

(defn drop-start
  "Removes chars from the left side of string by `pred`.
   The `pred` is a predicate function for chars to be removed."
  {:tag String
   :test #(test/are [form] form
            (= "test" (drop-start "test",,, char-whitespace?)) #_" 7 ns"
            (= "test" (drop-start "   test" char-whitespace?)) #_"30 ns"
            (= "test" (drop-start "   test" (char-not= \t)))
            (nil?,,,, (drop-start nil char-whitespace?)))}
  [^CharSequence s, pred]
  (when s
    (let [len (.length s)]
      (loop [index 0]
        (if (= len index)
          ""
          (if (pred (.charAt s (unchecked-int index)))
            (recur (unchecked-inc index))
            (.toString (.subSequence s (unchecked-int index) len))))))))

(defn drop-end
  "Removes chars from the right side of string by `pred`.
   The `pred` is a predicate function for chars to be removed."
  {:tag String
   :test #(test/are [form] form
            (= "test" (drop-end "test",,, char-whitespace?))
            (= "test" (drop-end "test   " char-whitespace?))
            (= "test" (drop-end "test   " (char-not= \t)))
            (nil?,,,, (drop-end nil char-whitespace?)))}
  [^CharSequence s, pred]
  (when s
    (loop [index (.length s)]
      (if (zero? index)
        ""
        (if (pred (.charAt s (unchecked-int (unchecked-dec index))))
          (recur (unchecked-dec index))
          (.toString (.subSequence s (unchecked-int 0) (unchecked-int index))))))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn take-before
  "Gets the substring before the first occurrence of a separator."
  {:tag String
   :test #(test/are [form] form
            (= "test" (take-before "test,string",,,,,,,,, ","))
            (= "test" (take-before "test::string::string" "::"))
            (= "test" (take-before "test" "::"))
            (= "test" (take-before "test" nil))
            (= "",,,, (take-before "test" ""))
            (= "",,,, (take-before "test" "test"))
            (= "",,,, (take-before "test" "t"))
            (nil?,,,, (take-before nil nil)))}
  [s separator]
  (StringUtils/substringBefore ^String s ^String separator))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn surround
  "Surrounds string `s` with `x` or `left`/`right`."
  {:tag String
   :test #(test/are [form] form
            (= "''", (surround nil "'"))
            (= "''", (surround "", "'"))
            (= "'s'" (surround "s" "'"))
            (= "'0'" (surround 0,, "'"))
            (= "()", (surround nil "(" ")"))
            (= "()", (surround "", "(" ")"))
            (= "(s)" (surround "s" "(" ")"))
            (= "(0)" (surround 0,, "(" ")")))}
  ([s x]
   (.concat (.concat ^String x (str s)) x))
  ([s left right]
   (.concat (.concat ^String left (str s)) right)))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(defn truncate
  "Truncates string `s` to the length `len`.
   Appends `suffix` at the end if specified."
  {:tag String
   :test #(test/are [form] form
            (= "12345",,, (truncate "1234567890" 5)),,,,,, #_"12 ns"
            (= "12345..." (truncate "1234567890" 5 "...")) #_"24 ns"
            (= "12345",,, (truncate "12345",,,,, 5))
            (= "12345",,, (truncate "12345",,,,, 5 "..."))
            (= "",,,,,,,, (truncate "",,,,,,,,,, 5))
            (= "",,,,,,,, (truncate "",,,,,,,,,, 5 "..."))
            (nil?,,,,,,,, (truncate nil,,,,,,,,, 5))
            (= "",,,,,,,, (truncate "12345",,,,, 0))
            (= "(1 2 ..." (truncate '(1 2 3 4 5) 5 "...")))}
  ([^Object s, ^long len]
   (when-some [^String s (some-> s .toString)]
     (if (< len (.length s))
       (.substring s (unchecked-int 0) (unchecked-int len))
       s)))
  ([^Object s, ^long len, suffix]
   (when-some [^String s (some-> s .toString)]
     (if (< len (.length s))
       (.concat (.substring s (unchecked-int 0) (unchecked-int len)) suffix)
       s))))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
