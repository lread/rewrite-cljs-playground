(ns ^:no-doc ^{:added "0.4.0"} rewrite-cljc.node.protocols
  (:require [clojure.string :as string]
            [rewrite-cljc.interop :as interop]
            #?(:clj [rewrite-cljc.potemkin.clojure :refer [defprotocol+]]))
  #?(:cljs (:require-macros [rewrite-cljc.potemkin.cljs :refer [defprotocol+]])))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defprotocol+ Node
  "Protocol for EDN/Clojure/ClojureScript nodes."
  (tag [node]
    "Returns keyword representing type of `node`.")
  (node-type [node]
    "Returns keyword representing the node type for `node`.
     Currently internal and used to support testing.")
  (printable-only? [node]
    "Return true if `node` cannot be converted to an s-expression element.")
  (sexpr [node] [node opts]
    "Return `node` converted to form.

     Optional `opts` can specify:
     - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-introduction.adoc#namespaced-elements)")
  (length [node]
    "Return number of characters for the string version of `node`.")
  (string [node]
    "Return the string version of `node`."))

(extend-protocol Node
  #?(:clj Object :cljs default)
  (tag [_this] :unknown)
  (node-type [_this] :unknown)
  (printable-only? [_this] false)
  (sexpr
    ([this] this)
    ([this _opts] this))
  (length [this] (count (string this)))
  (string [this] (pr-str this)))

(defn sexprs
  "Return forms for `nodes`. Nodes that do not represent s-expression are skipped.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-introduction.adoc#namespaced-elements)"
  ([nodes]
   (sexprs nodes {}))
  ([nodes opts]
   (->> nodes
        (remove printable-only?)
        (map #(sexpr % opts)))))

(defn ^:no-doc sum-lengths
  "Return total string length for `nodes`."
  [nodes]
  (reduce + (map length nodes)))

(defn ^:no-doc concat-strings
  "Return string version of `nodes`."
  [nodes]
  (reduce str (map string nodes)))

;; ## Inner Node

(defprotocol+ InnerNode
  "Protocol for non-leaf EDN/Clojure/ClojureScript nodes."
  (inner? [node]
    "Returns true if `node` can have children.")
  (children [node]
    "Returns child nodes for `node`.")
  (replace-children [node children]
    "Returns `node` replacing current children with `children`.")
  (leader-length [node]
    "Returns number of characters before children for `node`."))

(extend-protocol InnerNode
  #?(:clj Object :cljs default)
  (inner? [_this] false)
  (children [_this]
    (throw (ex-info "unsupported operation" {})))
  (replace-children [_this _children]
    (throw (ex-info "unsupported operation" {})))
  (leader-length [_this]
    (throw (ex-info "unsupported operation" {}))))

(defn child-sexprs
  "Returns children for `node` converted to Clojure forms.

  Optional `opts` can specify:
  - `:auto-resolve` specify a function to customize namespaced element auto-resolve behavior, see [docs on namespaced elements](/doc/01-introduction.adoc#namespaced-elements)"
  ([node]
   (child-sexprs node {}))
  ([node opts]
   (when (inner? node)
     (sexprs (children node) opts))))


(defn node?
  "Returns true if `x` is a rewrite-cljc created node."
  [ x ]
  (not= :unknown (tag x)))

;; TODO: probably not the right spot for this, this more of a default config
(defn default-auto-resolve [alias]
  (if (= :current alias)
    '?_current-ns_?
    (symbol (str "??_" alias "_??"))))

;; ## Coerceable

(defprotocol+ NodeCoerceable
  "Protocol for values that can be coerced to nodes."
  (coerce [form] "Coerce `form` to node."))

(defprotocol+ MapQualifiable
  "Protocol for nodes that can be namespaced map qualified"
  (map-context-apply [node map-qualifier]
    "Applies `map-qualifier` context to `node`")
  (map-context-clear [node]
    "Removes map-qualifier context for `node`"))

(defn- ^:no-doc node->string
  #?(:clj ^String [node]
     :cljs ^string [node])
  (let [n (str (if (printable-only? node)
                 (pr-str (string node))
                 (string node)))
        n' (if (re-find #"\n" n)
             (->> (string/replace n #"\r?\n" "\n  ")
                  (interop/simple-format "\n  %s\n"))
             (str " " n))]
    (interop/simple-format "<%s:%s>" (name (tag node)) n')))

#?(:clj
   (defn ^:no-doc write-node
     [^java.io.Writer writer node]
     (.write writer (node->string node))))

#?(:clj
   (defmacro ^:no-doc make-printable-clj!
     [class]
     `(defmethod print-method ~class
        [node# w#]
        (write-node w# node#)))
   :cljs
   (defn ^:no-doc make-printable-cljs!
     [obj]
     (extend-protocol IPrintWithWriter
       obj
       (-pr-writer [o writer _opts]
         (-write writer (node->string o))))))

(defn make-printable! [obj]
  #?(:clj (make-printable-clj! obj)
     :cljs (make-printable-cljs! obj)))

;; ## Helpers

(defn ^:no-doc without-whitespace
  [nodes]
  (remove printable-only? nodes))

(defn  ^:no-doc assert-sexpr-count
  [nodes c]
  (assert
   (= (count (without-whitespace nodes)) c)
   (interop/simple-format "can only contain %d non-whitespace form%s."
                          c (if (= c 1) "" "s"))))

(defn ^:no-doc assert-single-sexpr
  [nodes]
  (assert-sexpr-count nodes 1))

(defn ^:no-doc extent
  "A node's extent is how far it moves the \"cursor\".
  Rows are simple - if we have x newlines in the string representation, we
  will always move the \"cursor\" x rows.
  Columns are strange.  If we have *any* newlines at all in the textual
  representation of a node, following nodes' column positions are not
  affected by our startting column position at all.  So the second number
  in the pair we return is interpreted as a relative column adjustment
  when the first number in the pair (rows) is zero, and as an absolute
  column position when rows is non-zero."
  [node]
  (let [{:keys [row col next-row next-col]} (meta node)]
    (if (and row col next-row next-col)
      [(- next-row row)
       (if (= row next-row row)
         (- next-col col)
         next-col)]
      (let [s (string node)
            rows (->> s (filter (partial = \newline)) count)
            cols (if (zero? rows)
                   (count s)
                   (->> s
                     reverse
                     (take-while (complement (partial = \newline)))
                     count
                     inc))]
        [rows cols]))))

(defn ^:no-doc +extent
  [[row col] [row-extent col-extent]]
  [(+ row row-extent)
   (cond-> col-extent (zero? row-extent) (+ col))])

(def ^{:dynamic true
       :doc "The Clojure reader can add positional metadata.
            This metadata is not in the original source, and therefore assumed to be uninteresting to users of rewrite-cljc.
            Clojure will add `:line` and `:column` to quoted lists.
            Sci, and therefore Babashka does the same for all elements that accept metadata and adds `:end-line` and `:end-column`"}
  *elide-metadata* [:line :column :end-line :end-column])

(defn form-meta
  "Same as `clojure.core/meta` but respects [[rewrite-cljc/*elide-metadata*]].
  Use when you want to omit reader generated metadata on forms."
  [form]
  (apply dissoc (meta form) *elide-metadata*))
