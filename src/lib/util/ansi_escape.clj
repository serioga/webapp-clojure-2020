(ns lib.util.ansi-escape
  "ANSI color escape sequences.
  See https://stackoverflow.com/questions/4842424/list-of-ansi-color-escape-sequences.")

(set! *warn-on-reflection* true)

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

;;; Font effects.

(def ^:const reset "Reset/Normal." "\033[0m")

;;; 4-bit colors.

(def ^:const fg-black,,,, "Black FG color.",,,,,,,,, "\033[30m")
(def ^:const fg-black-b,, "Bright Black FG color.",, "\033[90m")
(def ^:const fg-red,,,,,, "Red FG color.",,,,,,,,,,, "\033[31m")
(def ^:const fg-red-b,,,, "Bright Red FG color.",,,, "\033[91m")
(def ^:const fg-green,,,, "Green FG color.",,,,,,,,, "\033[32m")
(def ^:const fg-green-b,, "Bright Green FG color.",, "\033[92m")
(def ^:const fg-yellow,,, "Yellow FG color.",,,,,,,, "\033[33m")
(def ^:const fg-yellow-b, "Bright Yellow FG color.", "\033[93m")
(def ^:const fg-blue,,,,, "Blue FG color.",,,,,,,,,, "\033[34m")
(def ^:const fg-blue-b,,, "Bright Blue FG color.",,, "\033[94m")
(def ^:const fg-magenta,, "Magenta FG color.",,,,,,, "\033[35m")
(def ^:const fg-magenta-b "Bright Magenta FG color." "\033[95m")
(def ^:const fg-cyan,,,,, "Cyan FG color.",,,,,,,,,, "\033[36m")
(def ^:const fg-cyan-b,,, "Bright Cyan FG color.",,, "\033[96m")

;;••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
