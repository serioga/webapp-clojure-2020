(ns lib.clojure-string.core
  "Extension of `clojure.string`. Similar to cuerdas, superstring etc."
  (:refer-clojure :exclude [concat empty? not-empty replace])
  (:require [clojure.string :as string]
            [clojure.test :as test])
  (:import (org.apache.commons.lang3 StringUtils)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/with-test

  (defn empty?
    "True if string `s` is nil or has zero length."
    [s]
    (StringUtils/isEmpty s))

  (test/are [expr result] (= result expr)
    (empty? nil) #_=> true
    (empty? ""), #_=> true
    (empty? "-") #_=> false))

(test/with-test

  (defn not-empty
    "If `s` is empty, returns nil, else `s`."
    [s]
    (when-not (empty? s) s))

  (test/are [expr result] (= result expr)
    (not-empty nil) #_=> nil
    (not-empty ""), #_=> nil
    (not-empty "-") #_=> "-"))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/with-test

  (defn concat
    "Concatenates strings using native `.concat`.
     Works with strings only."
    {:tag String}
    ([a b] (if a (if b (.concat ^String a b), a)
                 (or b nil)))
    ([a b c] (-> a (concat b) (concat c)))
    ([a b c d] (-> a (concat b) (concat c) (concat d)))
    ([a b c d e] (-> a (concat b) (concat c) (concat d) (concat e))))

  (test/are [expr result] (= result expr)
    (concat "0123456789", "ABCDE"),,,,,,,, #_=> "0123456789ABCDE"
    (concat "0123456789", nil),,,,,,,,,,,, #_=> "0123456789"
    (concat nil,,,,,,,,,, "ABCDE"),,,,,,,, #_=> "ABCDE"
    (concat "A",,,,,,,,,, "B" "C" "D" "E") #_=> "ABCDE"
    (concat nil,,,,,,,,,, nil),,,,,,,,,,,, #_=> nil))

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

(test/with-test

  (defn only-chars?
    "True if `s` contains only chars satisfying `pred`. False when `s` is empty."
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

  (test/are [expr result] (= result expr)
    (only-chars? nil,,, char-digit?) #_=> false
    (only-chars? "",,,, char-digit?) #_=> false
    (only-chars? "---", char-digit?) #_=> false
    (only-chars? "123", char-digit?) #_=> true))

(test/with-test

  (defn numeric?
    "True if `s` contains only digits."
    [s]
    (StringUtils/isNumeric s))

  (test/are [expr result] (= result expr)
    (numeric? "1234567890"), #_=> true
    (numeric? "1234567890x") #_=> false
    (numeric? nil),,,,,,,,,, #_=> false
    (numeric? ""),,,,,,,,,,, #_=> false))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/with-test

  (defn strip-start
    "Strips any of a set of characters from the start of a String.
     A `nil` input String returns `nil`.
     An empty string (\"\") input returns the empty string.
     Strips whitespaces if the string `strip-chars` is not specified."
    {:tag String}
    ([s] (StringUtils/stripStart s nil))
    ([s, strip-chars] (StringUtils/stripStart s strip-chars)))

  (test/are [expr result] (= result expr)
    (strip-start "test",,,, " "),, #_=> "test"
    (strip-start "   test", " "),, #_=> "test"
    (strip-start "   te  ", " "),, #_=> "te  "
    (strip-start "   test"),,,,,,, #_=> "test"
    (strip-start "///test", "/"),, #_=> "test"
    (strip-start "☺☺☺test", "☺"),, #_=> "test"
    (strip-start "/?☺test", "/?☺") #_=> "test"
    (strip-start nil "/"),,,,,,,,, #_=> nil))

(test/with-test

  (defn strip-end
    "Strips any of a set of characters from the end of a String.
     A `nil` input String returns `nil`.
     An empty string (\"\") input returns the empty string.
     Strips whitespaces if the string `strip-chars` is not specified."
    {:tag String}
    ([s] (StringUtils/stripEnd s nil))
    ([s, strip-chars] (StringUtils/stripEnd s strip-chars)))

  (test/are [expr result] (= result expr)
    (strip-end "test",,,, " "),, #_=> "test"
    (strip-end "test   ", " "),, #_=> "test"
    (strip-end "  st   ", " "),, #_=> "  st"
    (strip-end "test   "),,,,,,, #_=> "test"
    (strip-end "test///", "/"),, #_=> "test"
    (strip-end "test☺☺☺", "☺"),, #_=> "test"
    (strip-end "test/?☺", "/?☺") #_=> "test"
    (strip-end nil "/"),,,,,,,,, #_=> nil))

(test/with-test

  (defn drop-start
    "Removes chars from the left side of string by `pred`.
     The `pred` is a predicate function for chars to be removed."
    {:tag String}
    [^CharSequence s, pred]
    (when s
      (let [len (.length s)]
        (loop [index 0]
          (if (= len index)
            ""
            (if (pred (.charAt s (unchecked-int index)))
              (recur (unchecked-inc index))
              (.toString (.subSequence s (unchecked-int index) len))))))))

  (test/are [expr result] (= result expr)
    (drop-start "test",,,, char-whitespace?) #_=> "test" #_" 7 ns"
    (drop-start "   test", char-whitespace?) #_=> "test" #_"30 ns"
    (drop-start "   test", (char-not= \t)),, #_=> "test"
    (drop-start nil,,,,,,, char-whitespace?) #_=> nil))

(test/with-test

  (defn drop-end
    "Removes chars from the right side of string by `pred`.
     The `pred` is a predicate function for chars to be removed."
    {:tag String}
    [^CharSequence s, pred]
    (when s
      (loop [index (.length s)]
        (if (zero? index)
          ""
          (if (pred (.charAt s (unchecked-int (unchecked-dec index))))
            (recur (unchecked-dec index))
            (.toString (.subSequence s (unchecked-int 0) (unchecked-int index))))))))

  (test/are [expr result] (= result expr)
    (drop-end "test",,,, char-whitespace?) #_=> "test"
    (drop-end "test   ", char-whitespace?) #_=> "test"
    (drop-end "test   ", (char-not= \t)),, #_=> "test"
    (drop-end nil,,,,,,, char-whitespace?) #_=> nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/with-test

  (defn take-before
    "Gets the substring before the first occurrence of a separator."
    {:tag String}
    [s separator]
    (StringUtils/substringBefore ^String s ^String separator))

  (test/are [expr result] (= result expr)
    (take-before "test,string",,,,,,,,,, ","),,, #_=> "test"
    (take-before "test::string::string", "::"),, #_=> "test"
    (take-before "test",,,,,,,,,,,,,,,,, "::"),, #_=> "test"
    (take-before "test",,,,,,,,,,,,,,,,, nil),,, #_=> "test"
    (take-before "test",,,,,,,,,,,,,,,,, ""),,,, #_=> ""
    (take-before "test",,,,,,,,,,,,,,,,, "test") #_=> ""
    (take-before "test",,,,,,,,,,,,,,,,, "t"),,, #_=> ""
    (take-before nil,,,,,,,,,,,,,,,,,,,, nil),,, #_=> nil))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/with-test

  (defn surround
    "Surrounds string `s` with `x` or `left`/`right`."
    {:tag String}
    ([s x] (.concat (.concat ^String x (str s)) x))
    ([s left right] (.concat (.concat ^String left (str s)) right)))

  (test/are [expr result] (= result expr)
    (surround nil, "'"),,,,, #_=> "''"
    (surround "",, "'"),,,,, #_=> "''"
    (surround "s", "'"),,,,, #_=> "'s'"
    (surround 0,,, "'"),,,,, #_=> "'0'"
    (surround nil, "(", ")") #_=> "()"
    (surround "",, "(", ")") #_=> "()"
    (surround "s", "(", ")") #_=> "(s)"
    (surround 0,,, "(", ")") #_=> "(0)"))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

(test/with-test

  (defn truncate
    "Truncates string `s` to the length `len`.
     Appends `suffix` at the end if specified."
    {:tag String}
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

  (test/are [expr result] (= result expr)
    (truncate "1234567890", 5),,,,,,, #_=> "12345",,, #_"12 ns"
    (truncate "1234567890", 5, "...") #_=> "12345..." #_"24 ns"
    (truncate "12345",,,,,, 5),,,,,,, #_=> "12345"
    (truncate "12345",,,,,, 5, "...") #_=> "12345"
    (truncate "",,,,,,,,,,, 5),,,,,,, #_=> ""
    (truncate "",,,,,,,,,,, 5, "...") #_=> ""
    (truncate nil,,,,,,,,,, 5),,,,,,, #_=> nil
    (truncate "12345",,,,,, 0),,,,,,, #_=> ""
    (truncate '(1 2 3 4 5), 5, "...") #_=> "(1 2 ..."))

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
