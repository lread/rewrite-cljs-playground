(ns rewrite-clj.doo-test-runner
  (:require [doo.runner :refer-macros [doo-all-tests]]
            [rewrite-clj.examples.cljx-test]
            [rewrite-clj.impl.custom-zipper.core-test]
            [rewrite-clj.impl.custom-zipper.utils-test]
            [rewrite-clj.impl.node.coerce-test]
            [rewrite-clj.impl.node.generators]
            [rewrite-clj.impl.node.integer-test]
            [rewrite-clj.impl.node.node-test]
            [rewrite-clj.impl.potemkin-test]
            [rewrite-clj.impl.zip.base-test]
            [rewrite-clj.impl.zip.edit-test]
            [rewrite-clj.impl.zip.find-test]
            [rewrite-clj.impl.zip.insert-test]
            [rewrite-clj.impl.zip.move-test]
            [rewrite-clj.impl.zip.remove-test]
            [rewrite-clj.impl.zip.seq-test]
            [rewrite-clj.impl.zip.subedit-test]
            [rewrite-clj.impl.zip.walk-test]
            [rewrite-clj.impl.zip.whitespace-test]
            [rewrite-clj.node-test]
            [rewrite-clj.paredit-test]
            [rewrite-clj.parser-test]
            [rewrite-clj.regression-test]
            [rewrite-clj.transform-test]
            [rewrite-clj.zip-test]))

(doo-all-tests #"(rewrite-clj\..*-test)")
