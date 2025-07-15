(ns gemlog-indexer.gmi
  (:require [clojure.java.io :as io]
            [clojure.string  :as string]))

(defn- sanitise-filename
  [filename]
  (string/replace filename #" " "%20"))

(defn write-fixed-header
  [writer]
  (.write writer "# Spiky Dinosaur\n\n")
  (.write writer "## Log entries\n\n"))

(defn write-entry
  [writer entry]
  (.write writer (str "=> "
                      (sanitise-filename (:filename entry))
                      " "
                      (:created-date entry)
                      " "
                      (:title entry)
                      "\n")))

(defn create-gmi-index-file
  [gemlog-metadata options]
  (with-open [writer (io/writer (:gmi-index-file options) :append false)]
    (write-fixed-header writer)
    (doseq [entry (reverse (sort-by :created-date gemlog-metadata))]
      (write-entry writer entry))))

  

