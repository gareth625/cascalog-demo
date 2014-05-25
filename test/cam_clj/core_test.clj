(ns cam-clj.core
  (:require [cam-cli.core :refer :all]
            [cascalog.api :refer :all]
            [clojure.test :refer :all]
            [midje.cascalog :refer [produces]]
            [midje.sweet :refer [fact tabular throws]]))
