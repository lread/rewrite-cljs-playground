(ns rewrite-clj.parser
  "Parse Clojure/ClojureScript/EDN source code to nodes.

  After parsing, the typical next step is [[rewrite-clj.zip/edn]] to create zipper.

  Alternatively consider parsing and zipping in one step from [[rewrite-clj.zip/of-string]] or [[rewrite-clj.zip/of-file]]."
  (:require [rewrite-clj.parser.core :as p]
            [rewrite-clj.node :as node]
            [rewrite-clj.reader :as reader]))

;; ## Parser Core

(defn ^:no-doc parse
  "Parse next form from the given reader."
  [#?(:cljs ^not-native reader :default reader)]
  (p/parse-next reader))

(defn ^:no-doc parse-all
  "Parse all forms from the given reader."
  [#?(:cljs ^not-native reader :default reader)]
  (let [nodes (->> (repeatedly #(parse reader))
                   (take-while identity)
                   (doall))]
    (with-meta
      (node/forms-node nodes)
      (meta (first nodes)))))

;; ## Specialized Parsers

(defn parse-string
  "Parse first form in the given string."
  [s]
  (parse (reader/string-reader s)))

(defn parse-string-all
  "Parse all forms in the given string."
  [s]
  (parse-all (reader/string-reader s)))

#?(:clj
   (defn parse-file
     "Parse first form from the given file."
     [f]
     (let [r (reader/file-reader f)]
       (with-open [_ ^java.io.Closeable (.-rdr r)]
         (parse r)))))

#?(:clj
   (defn parse-file-all
     "Parse all forms from the given file."
     [f]
     (let [r (reader/file-reader f)]
       (with-open [_ ^java.io.Closeable (.-rdr r)]
         (parse-all r)))))