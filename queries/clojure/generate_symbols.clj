#!/bin/sh
#_(

   #_DEPS is same format as deps.edn. Multiline is okay.
   DEPS='
   {:deps {}}
   '

   #_You can put other options here
   OPTS='
   -J-Xms1024m -J-Xmx1024m
   '

exec clojure $OPTS -Sdeps "$DEPS" "$0" "$@"

)

(ns generate-symbols
  (:require
   [clojure.string :as str]))

(defn is-symbol-a-macro? [[_ v]]
  (:macro (meta v)))

(defn is-symbol-special? [[s _]]
  (special-symbol? @(resolve s)))

(defn is-symbol-a-fn? [[s _]]
  (fn? @(resolve s)))

(defn escape [s]
  (str "\"" s "\""))

(defn spit-symbols [file coll]
  (->> (map name coll)
       (sort)
       (distinct)
       (map escape)
       (interpose " ")
       (apply str)
       (spit file)))

(defn is-def-type-symbol? [[s _]]
  (let [sym-name (name s)]
    (and (or (str/starts-with? sym-name "def") (contains? #{"declare" "ns"} sym-name))
         (not (contains? #{"default-data-readers" "defs"} sym-name)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def defs (->> (ns-publics 'clojure.core)
               (filter is-symbol-a-macro?)
               (filter is-symbol-a-fn?)
               (filter is-def-type-symbol?)
               (map first)
               (into ['def])))

(def macros (->> (ns-publics 'clojure.core)
                 (filter #(or (is-symbol-a-macro? %) (is-symbol-special? %)))
                 (filter (complement is-def-type-symbol?))
                 (map first)
                 (into ['if 'do 'let 'quote 'var 'fn 'fn* 'loop 'recur 'throw 'try 'catch
                        'monitor-enter 'monitor-exit
                        '.])))

(def functions (->> (ns-publics 'clojure.core)
                    (filter (complement is-symbol-a-macro?))
                    (filter is-symbol-a-fn?)
                    (map first)))

(spit-symbols "clojure.core.functions.txt" functions)
(spit-symbols "clojure.core.macros.txt" macros)
(spit-symbols "clojure.core.defs.txt" defs)
