(ns rewrite-cljc.zip-test
  "This test namespace originated from rewrite-cljs."
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [rewrite-cljc.node :as n]
            [rewrite-cljc.zip :as z]))

(deftest of-string-simple-sexpr
  (let [sexpr "(+ 1 2)"]
   (is (= sexpr (-> sexpr z/of-string z/root-string)))))

(deftest manipulate-sexpr
  (let [sexpr
        (string/join
         "\n" [""
               " ^{:dynamic true} (+ 1 1"
               "   (+ 2 2)"
               "   (reduce + [1 3 4]))"])
        expected
        (string/join
         "\n" [""
               " ^{:dynamic true} (+ 1 1"
               "   (+ 2 2)"
               "   (reduce + [6 7 [1 2]]))"])]
    (is (= expected (-> (z/of-string sexpr {:track-position? true})
                        (z/find-tag-by-pos {:row 4 :col 19} :vector) ;; should find [1 3 4] col 19 points to element 4 in vector
                        (z/replace [5 6 7])                          ;; replaces [1 3 4] with [5 6 7]
                        (z/append-child [1 2])                       ;; appends [1 2] to [5 6 7] giving [5 6 [1 2]]
                        z/down                                       ;; navigate to 5
                        z/remove                                     ;; remove 5 giving [6 7 [1 2]]
                        z/root-string)))))

(deftest namespaced-keywords
  (is (= ":dill" (-> ":dill" z/of-string z/root-string)))
  (is (= "::dill" (-> "::dill" z/of-string z/root-string)))
  (is (= ":dill/dall" (-> ":dill/dall" z/of-string z/root-string)))
  (is (= "::dill/dall" (-> "::dill/dall" z/of-string z/root-string)))
  (is (= ":%dill.*" (-> ":%dill.*" z/of-string z/root-string))))


(deftest sexpr-udpates-correctly-for-namespaced-map-keys
  (testing "on parse"
    (is (= '(:prefix/a 1 :prefix/b 2 prefix/c 3)
           (-> "#:prefix {:a 1 :b 2 c 3}"
               z/of-string
               z/down
               z/rightmost
               z/child-sexprs))))
  (testing "on insert new key val"
    (is (= '(:prefix/a 1 :prefix/b 2 prefix/c 3 prefix/d 4)
           (-> "#:prefix {:a 1 :b 2 c 3}"
               z/of-string
               z/down
               z/rightmost
               (z/append-child 'd)
               (z/append-child 4)
               z/up ;; changes and also nsmap context are applied when moving up to nsmap
               z/down
               z/rightmost
               z/child-sexprs))))
  (testing "on update existing key val"
    (is (= '(:prefix/a 1 :prefix/b2 2 prefix/c 3)
           (-> "#:prefix {:a 1 :b 2 c 3}"
               z/of-string
               z/down
               z/rightmost
               z/down
               z/right
               z/right
               (z/replace :b2)
               z/up ;; changes and also nsmap context are applied when moving up to nsmap
               z/up
               z/down
               z/rightmost
               z/child-sexprs))))
  (testing "on update creating unbalanced map (which rewrite-clj allows) context is cleared/applied as appropriate"
    (is (= '(:prefix/hi :a prefix/b :c prefix/d e prefix/f)
           (-> "#:prefix {:a b :c d e f}"
               z/of-string
               z/down
               z/rightmost
               (z/insert-child :hi)
               z/up ;; changes and also nsmap context are applied when moving up to nsmap
               z/down
               z/rightmost
               z/child-sexprs))))
  (testing "namespaced map qualifier can be changed and affect sexpr of its map keys"
    (is (= '(:??_ns-alias_??/a 1 :??_ns-alias_??/b 2 :c 3)
           (-> "#:prefix {:a 1 :b 2 :_/c 3}"
               z/of-string
               z/down
               (z/replace (n/map-qualifier-node true "ns-alias"))
               z/up
               z/down
               z/rightmost
               z/child-sexprs))))
  (testing "node context can be be explicitly removed when moving node out of namespaced map"
    (is (= '[{:prefix/b 2 :prefix/c 3}
             {:a 1 :z 99}]
           (let [zloc (-> "[#:prefix {:a 1 :b 2 :c 3}{:z 99}]"
                          z/of-string
                          z/down
                          z/down
                          z/rightmost
                          z/down)
                 move-me1 (-> zloc z/node n/map-context-clear) ;; if we don't clear the map context it will remain
                 zloc (-> zloc z/remove z/down)
                 move-me2 (-> zloc z/node)
                 zloc (z/remove zloc)]
             (-> zloc
                 z/up
                 z/right
                 (z/insert-child move-me2)
                 (z/insert-child move-me1)
                 z/up
                 z/sexpr)))))
  (testing "node context can be explicitly reapplied to entire zloc downward"
    (is (= '[{:prefix/b 2 :prefix/c 3}
             {:a 1 :z 99}]
           (let [zloc (-> "[#:prefix {:a 1 :b 2 :c 3}{:z 99}]"
                          z/of-string
                          z/down
                          z/down
                          z/rightmost
                          z/down)
                 move-me1 (-> zloc z/node)  ;; notice we don't clear context here
                 zloc (-> zloc z/remove z/down)
                 move-me2 (-> zloc z/node)
                 zloc (z/remove zloc)]
             (-> zloc
                 z/up
                 z/right
                 (z/insert-child move-me2)
                 (z/insert-child move-me1)
                 z/up
                 z/reapply-context ;; but we do reapply context to tree before doing a sexpr
                 z/sexpr))))) )
