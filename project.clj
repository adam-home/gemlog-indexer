(defproject gemlog-indexer "0.1.2"
  :description "Simple atom.xml generator for gemlogs"
  :url "http://spikydinosaur.com"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure   "1.12.1"]
                 [org.clojure/tools.cli "1.1.230"]]
  :main ^:skip-aot gemlog-indexer.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
