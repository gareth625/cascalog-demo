(ns cam-clj.core
  (:require [cam-clj.core :refer :all]
            [cascalog.api :refer :all]
            [clojure.test :refer :all]
            [midje.cascalog :refer [produces]]
            [midje.sweet :refer [fact tabular throws]] :reload))

(tabular
 (fact
  "Midje has been extended to provide a sink for Cascalog streams, in
  this case I'm demonstrating 'produces' sink which is used to check
  source stream is as expected."
  ?source-tap => (produces ?expected-data))

 ; I've opted for a trivial test case again where the source is sent
 ; straight to the sink and thus input and output is obviously the same.
 ?source-tap              ?expected-data
 source-data              source-data
 source-tap-with-sparkles [["_data with_"   "_TWO COLUMNS_"]
                           ["_and it also_" "_HAS TWO ROWS_"]])
