(defproject cascalog-demo "0.1.0-SNAPSHOT"
  :description "The Cascalog demonstration for the Cambridge-NonDysFunctional-Programmers Meetup."
  :url "https://github.com/gareth625/cascalog-demo"
  :main cam-cli.core
  :aot [cam-clj.core]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cascalog "2.1.0"]
                 [org.clojure/tools.cli "0.3.1"]]
  :dev-dependencies [[cascalog/midje-cascalog "1.10.1"]
                     [midje "1.6.3"]]
  :plugins [[lein-gorilla "0.2.0"]
            [lein-kibit "0.0.8"]
            [lein-marginalia "0.7.1"]
            [lein-midje "3.0.1"]]
  :profiles {:provided {:dependencies [[org.apache.hadoop/hadoop-core "1.2.1"]]}})
