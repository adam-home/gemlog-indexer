(ns gemlog-indexer.core
  (:require [gemlog-indexer.atom :as atom]
            [gemlog-indexer.gmi  :as gmi]
            [clojure.string      :as string]
            [clojure.java.io     :as io]
            [clojure.tools.cli   :as cli])
  (:gen-class))

(def verbose (atom false))

(def cli-options
  [["-d" "--gemlog-dir" "Directory containing gemlogs"
    :id       :gemlog-dir
    :required "DIR"
    :default  "resources/gemlog"]
   ["-t" "--atom-template" "atom.xml template file"
    :id       :atom-template
    :required "FILE"
    :default  "resources/atom-template.xml"]
   ["-a" "--atom-file" "atom file"
    :id       :atom-file
    :required "FILE"
    :default  "atom.xml"]
   ["-g" "--gemini-index-file" "gemini index file"
    :id       :gmi-index-file
    :required "FILE"
    :default  "index.gmi"]
   ["-v" "--verbose" "verbose output"
    :id       :verbose
    :default  false]
   ["-h" "--help"]])

(defn strip-dir
  [filename dir]
  (if (string/starts-with? filename dir)
    (subs filename (inc (count dir)))
    filename))

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
  [file gemlog-dir]
  (when @verbose
    (println (str "Adding " (.getPath file))))
  {:filename     (strip-dir (.getPath file) gemlog-dir)
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
                               (println "gemini index file:" (:gmi-index-file options))
                               options)
          :else              options)))

(defn -main
  [& args]
  (let [options (validate-options args)]
    (when options
      (let [gemlog-files      (list-gemlog-files (:gemlog-dir options))
            gemlog-metadata   (map #(get-gemlog-metadata % (:gemlog-dir options)) gemlog-files)]
        (atom/create-atom-file gemlog-metadata options)
        (gmi/create-gmi-index-file gemlog-metadata options)))))
