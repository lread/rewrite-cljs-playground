(ns ^:no-doc rewrite-clj.internal.parser.utils
  (:require [clojure.tools.reader.reader-types :as r]
            [rewrite-clj.internal.interop :as interop]))

;; TODO: this file is a bit odd, it repeats code from other places
(defn whitespace?
  "Check if a given character is a whitespace."
  [c]
  (interop/clojure-whitespace? c))

(defn linebreak?
  "Check if a given character is a linebreak."
  [c]
  (and c (or (= c \newline) (= c \return))))

(defn space?
  "Check if a given character is a non-linebreak whitespace."
  [c]
  (and (not (linebreak? c)) (whitespace? c)))

(defn ignore
  "Ignore next character of Reader."
  [reader]
  (r/read-char reader)
  nil)

(defn throw-reader
  [reader & msg]
  (let [c (r/get-column-number reader)
        l (r/get-line-number reader)]
    (throw (ex-info
            (str (apply str msg) " [at line " l ", column " c "]") {}))))

(defn read-eol
  [reader]
  (loop [char-seq []]
    (if-let [c (r/read-char reader)]
      (if (linebreak? c)
        (apply str (conj char-seq c))
        (recur (conj char-seq c)))
      (apply str char-seq))))
