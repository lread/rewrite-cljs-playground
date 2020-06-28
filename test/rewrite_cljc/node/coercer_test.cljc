(ns rewrite-cljc.node.coercer-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-cljc.node.protocols :as node :refer [coerce]]
            [rewrite-cljc.parser :as p]))

(deftest t-sexpr->node->sexpr-roundtrip
  (are [?sexpr expected-tag]
      (let [n (coerce ?sexpr)]
        (is (satisfies? node/Node n))
        (is (= expected-tag (node/tag n)))
        (is (string? (node/string n)))
        (is (= ?sexpr (node/sexpr n)))
        (is (not (meta n)))
        (is (= (type ?sexpr) (type (node/sexpr n)))))
    ;; numbers
    3                      :token
    3N                     :token
    3.14                   :token
    3.14M                  :token
    3e14                   :token

    ;; ratios are not valid in cljs
    #?@(:clj  [3/4         :token])

    ;; symbol/keyword/string/...
    'symbol                :token
    'namespace/symbol      :token
    :keyword               :token
    :1.5.1                 :token
    ::keyword              :token
    ::1.5.1                :token
    :namespace/keyword     :token
    ""                     :token
    "hello, over there!"   :token
    "multi\nline"          :token

    ;; seqs
    []                     :vector
    [1 2 3]                :vector
    ()                     :list
    '()                    :list
    '(1 2 3)               #?(:clj ;; clojure includes :line and :column metadata for non-empty quoted lists
                              :meta
                              :cljs ;; cljs does not include metadata for quoted lists
                              :list)
    (list 1 2 3)           :list
    #{}                    :set
    #{1 2 3}               :set

    ;; date
    #inst "2014-11-26T00:05:23" :token

    ;; map
    (hash-map)             :map
    (hash-map :a 0, :b 1)  :map))

(deftest t-sexpr->node->sexpr-roundtrip-for-regex
  (let [sexpr #"abc"
        n (coerce sexpr)]
    (is (satisfies? node/Node n))
    (is (string? (node/string n)))
    (is (= (str sexpr) (str (node/sexpr n))))
    (is (= (type sexpr) (type (node/sexpr n))))))

(deftest t-vars
  (let [n (coerce #'identity)]
    (is (satisfies? node/Node n))
    (is (= '(var #?(:clj clojure.core/identity :cljs cljs.core/identity)) (node/sexpr n)))))

(deftest t-nil
  (let [n (coerce nil)]
    (is (satisfies? node/Node n))
    (is (= nil (node/sexpr n)))
    (is (= n (p/parse-string "nil")))))

(defrecord Foo-Bar [a])

(deftest t-records
  (let [v (Foo-Bar. 0)
        n (coerce v)]
    (is (satisfies? node/Node n))
    (is (= :reader-macro (node/tag n)))
    (is (= (pr-str v) (node/string n)))))
