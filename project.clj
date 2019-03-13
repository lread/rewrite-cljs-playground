(defproject rewrite-cljs "0.4.5-SNAPSHOT"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/rundis/rewrite-cljs"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"
                  :exclusions [org.apache.ant/ant]]
                 [org.clojure/tools.reader "1.3.2"]]

  :source-paths ["src/clj/" "src/cljc" "src/cljs/"]

  :clean-targets ^{:protect false} [:target-path :compile-path "out"]

  :doo {:build "test"
        :karma {:config {"plugins" ["karma-junit-reporter"]
                         "reporters" ["progress" "junit"]
                         "junitReporter" {"outputDir" "target/out/test-results"}}}}

  :profiles {:dev
             {:dependencies [[org.clojure/test.check "0.9.0"]]
              :plugins [[lein-cljsbuild "1.1.7"]
                        [lein-doo "0.1.11"]]

              :cljsbuild {:builds
                          [{:id "test"
                            :source-paths ["test/cljs" "test/cljc"]
                            :compiler {:output-dir "target/cljsbuild/test/out"
                                       :output-to "target/cljsbuild/test/main.js"
                                       :main rewrite-clj.runner
                                       :source-map true
                                       :optimizations :none
                                       :pretty-print true}}]}}

             :doc {:plugins  [[funcool/codeina "0.1.0"
                               :exclusions [org.clojure/clojure]]]
                   :codeina {:sources ["src"]
                             :language :clojurescript
                             :src-dir-uri "https://github.com/rundis/rewrite-cljs/blob/master/"
                             :src-linenum-anchor-prefix "L"}}}

  :aliases {"auto-test" ["with-profile" "dev" "do" "clean," "cljsbuild" "auto" "test"]})
