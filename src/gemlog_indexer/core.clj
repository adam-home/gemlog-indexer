(ns gemlog-indexer.core
  (:require [gemlog-indexer.atom :as atom]
            [clojure.string      :as string]
            [clojure.java.io     :as io]
            [clojure.tools.cli   :as cli])
  (:gen-class))

(def verbose (atom false))

(def cli-options
  [["-d" nil "Directory containing gemlogs"
    :id       :gemlog-dir
    :required "DIR"
    :default  "resources/gemlog"]
   ["-t" nil "atom.xml template file"
    :id       :atom-template
    :required "FILE"
    :default  "resources/atom-template.xml"]
   ["-o" nil  "output file"
    :id       :atom-file
    :required "FILE"
    :default "atom.xml"]
   ["-v" nil "verbose output"
    :id       :verbose
    :default  false]
   ["-h" "--help"]])

(defn file-to-title
  "Given a file object, extract the title"
  [file]
  (-> (.getName file)
      (string/replace #"^.*?[^a-zA-Z0-9]+" "")
      (string/replace #"\.gmi$" "")))

(defn gemlog-get-title
  "The title is the first line of the file that starts with a single '#'"
  [file]
  (with-open [r (io/reader file)]
    (let [title (first (filter #(string/starts-with? % "#")
                               (line-seq r)))]
      (if title
        (string/replace title #"^#+\s+" "") ; Strip leading # and whitespace
        (file-to-title file))))) 

(defn gemlog-get-date
  "Extracts the date from a File object's filename.
  Directory paths should look like yyyy/mm/dd-name-of-gemini-file.gmi"
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
  "For the given gemfile (File), get data required for an index entry."
  [file]
  (when @verbose
    (println (str "Adding " (.getPath file))))
  {:filename     (.getPath file)
   :created-date (gemlog-get-date file)
   :title        (gemlog-get-title file)})

(defn is-gemlog-file?
  "Sanity check that the File is indeed a gemlog file"
  [file]
  (and (string/ends-with? (.getPath file) ".gmi")
       (gemlog-get-date file)))

(defn list-gemlog-files
  "Recursively list all gemlog files in the given directory"
  [dir]
  (->> (io/file dir)
       file-seq
       (filter #(.isFile %))
       (filter #(is-gemlog-file? %))))

(defn show-usage
  "Show summary of cli options"
  [summary]
  (println summary))

(defn validate-options
  "Parse and validate cli options"
  [args]
  (let [{:keys [options _arguments _errors summary]} (cli/parse-opts args cli-options)]

    (cond (:help options)    (do
                               (show-usage summary)
                               nil)
          (:verbose options) (do
                               (reset! verbose true)
                               (println "gemlog directory :" (:gemlog-dir options))
                               (println "atom template    :" (:atom-template options))
                               (println "atom output file :" (:atom-file options))
                               options)
          :else              options)))

(defn -main
  [& args]
  (let [options (validate-options args)]
    (when options
      (let [gemlog-files      (list-gemlog-files (:gemlog-dir options))
            gemlog-metadata   (map get-gemlog-metadata gemlog-files)]
        (atom/create-atom-file gemlog-metadata (:atom-template options) (:atom-file options))))))
