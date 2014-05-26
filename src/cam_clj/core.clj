(ns cam-clj.core
  (:require [cascalog.api :refer :all])
  (:gen-class))

; Here we define the simplest type of source tap which is just a tuple of tuples.
;
; Cascalog represents the data moving between taps as a tuple of tuples. Each
; tuple represents a row and each entry in the a row tuple represents the value
; that the column take in that row.
;
; When writing mapper and filter functions to manipulate your streams your
; functions will take a the current row, which could be in the form of a tuple
; i.e. '& args' and thus any number of columns; or it could have a defined
; argument list and expect specific columns.
(def source-data
  "The simplest form of aggregation function is given a tuple of tuples which
  is all of the rows and all of the columns.

  It may or may not help to try and imagine this as a CSV file e.g.
    _data with_,_two columns_
    _and it also_,_has two rows_"
  [["_dAtA wITh_"   "_twO cOlumns_"]
   ["_And it AlSo_" "_hAs two rows_"]])


(defn query-0
  []
  ; This reads backwards :)
  ; We use ?- macro to execute our query and the first argument to this macro
  ; is the sink tap.
  (?-

   ; stdout is a function provided by the Cascalog API that returns a sink
   ; connected to stdout. Thus the result of our query will be sent to the
   ; console.
   (stdout)

   ; The <- macro is used to define a query, where a query is a set of
   ; operations to transform the source data into the desired output for
   ; the sink.
   (<-

    ; In this case our output is just the two columns.
    [?column-one ?column-two]

    ; which have been sent from our source tap to variables called
    ; ?column-one and ?column-two. In this case, using naked tuples
    ; as the source, the order matters.
    (source-data :> ?column-one ?column-two))))

; Without all the annoying comments:
(defn query-0-again
  []
  (?- (stdout)
      (<- [?column-one ?column-two]
          (source-data :> ?column-one ?column-two))))

; (query-0)
; (query-0-again)

; Executing either of the above query gives us:
;   RESULTS
;   -----------------------
;   _dAtA wITh_   _twO cOlumns_
;   _And it AlSo_ _hAs two rows_
;   -----------------------

; A much more common way of writting the above is to use the ?<- macro which,
; as you might guess, defines both defines and executes the query i.e. it's a
; combination of the ?- and <- macros. It's slight less verbose.
(defn query-0-1
  []
  (?<- (stdout)
       [?column-one ?column-two]
       (source-data :> ?column-one ?column-two)))

; (query-0-1)

; This we can unit test... jumping to core_test.clj... and we're back.


(defn source-tap-with-sparkles
  "Using the <- marco we can define a source tap which might contain some data
  clean up before we return it to the user."
  []
  (<- [?column-one ?column-two]
      (source-data :> ?column-one ?column-two)))

(defn query-1
  []
  (?<- (stdout)
       [?column-one ?column-two]
       ((source-tap-with-sparkles) :> ?column-one ?column-two)))


(defn -main
  "The project's main function."
  [& args]
  (println "Hello cam-cli!"))
