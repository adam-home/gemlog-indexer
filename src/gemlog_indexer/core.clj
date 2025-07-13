(ns gemlog-indexer.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.xml :as xml])
  (:gen-class))

(def GEMLOG-DIR        "resources/gemlog")
(def ATOM-TEMPLATE     "resources/atom-template.xml")
(def ATOM-XML-FILENAME "atom.xml")

(defn file-to-title
  ;; Given a file object, extract the title
  [file]
  (-> (.getName file)
      (string/replace #"^.*?[^a-zA-Z0-9]+" "")
      (string/replace #"\.gmi$" "")))

(defn gemlog-get-title
  ;; The title is the first line of the file that starts with a single '#'
  [file]
  (with-open [r (io/reader file)]
    (let [title (first (filter #(string/starts-with? % "#")
                               (line-seq r)))]
      (if title
        (string/replace title #"^#+\s+" "") ; Strip leading # and whitespace
        (file-to-title file))))) 

(defn gemlog-get-date
  [file]
  (try
    (let [parts (reverse (string/split (.getPath file) #"/"))
          year  (nth parts 2)
          month (nth parts 1)
          day   (re-find #"^[0-9]+" (nth parts 0))]
      (format "%04d-%02d-%02dT00:00:00Z"
              (Integer/parseInt year)
              (Integer/parseInt month)
              (Integer/parseInt day)))
    (catch java.lang.Exception _e
      nil)))

(defn get-gemlog-metadata
  [file]
  {:filename     (.getPath file)
   :created-date (gemlog-get-date file)
   :title        (gemlog-get-title file)})

(defn metadata-to-atom
  [meta]
  {:tag :entry :content [
                         {:tag :id      :content [(:filename meta)]}
                         {:tag :updated :content [(:created-date meta)]}
                         {:tag :title   :content [(:title meta)]}]})

(defn is-gemlog-file?
  [file]
  (and (string/ends-with? (.getPath file) ".gmi")
       (gemlog-get-date file)))

(defn list-gemlog-files
  [dir]
  (->> (io/file dir)
       file-seq
       (filter #(.isFile %))
       (filter #(is-gemlog-file? %))))


(defn create-atom-content
  [gemlog-metadata]
  {:tag :entries :content (mapv metadata-to-atom gemlog-metadata)})
    

(defn -main
  [& _args]
  (let [gemlog-files      (list-gemlog-files GEMLOG-DIR)
        gemlog-metadata   (map get-gemlog-metadata gemlog-files)
        atom-content      (create-atom-content gemlog-metadata)
        xml-entries-str   (reduce str "" (map #(with-out-str (xml/emit-element %))
                                              (:content atom-content)))
        atom-template-str (slurp ATOM-TEMPLATE)]

    (spit ATOM-XML-FILENAME (string/replace-first atom-template-str "<placeholder/>" xml-entries-str))))
