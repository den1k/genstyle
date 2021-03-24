(ns genstyle.util
  (:require [clojure.string :as str]
            [medley.core :as md]
            [clojure.java.io :as io])
  (:import (java.util Date)))

(defn project
  ([f coll] (project {} f coll))
  ([to f coll] (into to (map f) coll)))

(defn project-as-keys
  ([key-fn coll]
   (project-as-keys {} key-fn coll))
  ([to key-fn coll]
   (project to (fn [x] [(key-fn x) x]) coll)))

(defn bound [num lower-bound upper-bound]
  (cond
    (<= lower-bound num upper-bound) num
    (< num lower-bound) lower-bound
    (> num upper-bound) upper-bound))

(defn round [points num]
  (let [pts (Math/pow 10 points)]
    (float (/ (Math/round (* pts num)) pts))))

(defn random-flip-sign [num]
  ((rand-nth [- +]) num))

(defn fn-map->transform
  ([fn-map]
   (fn [m]
     (reduce-kv
       (fn [out k f]
         (if-some [v (get m k)]
           (assoc out k (f v))
           out))
       m
       fn-map)
     ))
  ([fn-map m]
   ((fn-map->transform fn-map) m)))

(defn wrap
  ([around]
   (fn [s]
     (wrap s around around)))
  ([before after]
   (fn [s]
     (str before s after)))
  ([s before after]
   (str before s after)))

(def css-comment (wrap "/*\n" "\n*/\n\n"))

(def ^:private wrap-mustache (wrap "{{" "}}"))

(def ^:private mustache-regex
  #"\{\{[a-z0-9-_]+\}\}")

(defn mustache-replace [fmt-str replace-map]
  (let [rm (into {}
                 (map (fn [[k v]]
                        [(-> k name wrap-mustache)
                         (str v)])) replace-map)]
    (str/replace fmt-str mustache-regex rm)))

(def ^:private adjectives
  (delay
    (read-string (slurp (io/resource "words/adjectives.edn")))))

(def ^:private names
  (delay
    (read-string (slurp (io/resource "words/names.edn")))))

(defn name-gen
  ([] (name-gen (constantly false)))
  ([exists?]
   (let [nm (str (rand-nth @adjectives)
                 "-"
                 (rand-nth @names))]
     (if (exists? nm)
       (name-gen exists?)
       nm))))

(comment (name-gen))

(defn date-instant []
  (Date.))

(defn date-time []
  (.getTime (date-instant)))

(defn pretty-string [x]
  (str/trim (with-out-str (clojure.pprint/pprint x))))

(defn delete-directory-recursive
  "Recursively delete a directory."
  [file]
  (let [file (cond-> file (string? file) io/file)]
    (when (.exists file)
      (when (.isDirectory file)
        (doseq [file-in-dir (.listFiles file)]
          (delete-directory-recursive file-in-dir)))
      (io/delete-file file))))