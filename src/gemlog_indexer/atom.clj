(ns gemlog-indexer.atom
  (:require [clojure.string :as string]
            [clojure.xml    :as xml]))

(defn metadata-to-atom
  "Converts gemfile metadata to a clojure.xml formatted map."
  [gemlog-metadata]
  {:tag :entry :content [{:tag :id      :content [(:filename gemlog-metadata)]}
                         {:tag :updated :content [(:created-date gemlog-metadata)]}
                         {:tag :title   :content [(:title gemlog-metadata)]}]})

(defn create-atom-content
  "Create clojure.xml formatted map from a seq of gemlog metadata"
  [gemlog-metadata]
  {:tag :entries :content (mapv metadata-to-atom gemlog-metadata)})

(defn create-atom-file
  [gemlog-metadata atom-template atom-file]
  (let [atom-content      (create-atom-content gemlog-metadata)
        xml-entries-str   (reduce str "" (map #(with-out-str (xml/emit-element %))
                                              (:content atom-content)))
        atom-template-str (slurp atom-template)]
        
    (println (str "Processed " (count gemlog-metadata) " entries"))
    (spit atom-file (string/replace-first atom-template-str "<placeholder/>" xml-entries-str))))
