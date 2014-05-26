(ns cam-clj.core
  (:require [cascalog.api :refer :all]
            [clojure.string :as s])
  (:gen-class))

; -----------------------
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
   ["_some rows_"    nil]
   [nil             "_contain nils_"]
   [nil             nil]
   ["_And it AlSo_" "_hAs four rows_"]])

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

    ; In this case our output is just the two columns which are defined in the
    ; next line. Note that the leading exclamation mark '!' is important, this
    ; tells Cascalog the variables can be nil.
    [!column-one !column-two]

    ; which have been sent from our source tap to variables called
    ; !column-one and !column-two. In this case, using naked tuples
    ; as the source, the order matters.
    (source-data !column-one !column-two))))

; Without all the annoying comments:
(defn query-0-again
  []
  (?- (stdout)
      (<- [!column-one !column-two]
          (source-data !column-one !column-two))))

; (query-0)
; (query-0-again)

; Executing either of the above query gives us:
;   RESULTS
;   -----------------------
;   _dAtA wITh_   _twO cOlumns_
;   _some rows_   null
;   null          _contain nils_
;   null          null
;   _And it AlSo_ _hAs four rows_
;   -----------------------

; A much more common way of writting the above is to use the ?<- macro which,
; as you might guess, defines both defines and executes the query i.e. it's a
; combination of the ?- and <- macros. It's slight less verbose.
(defn query-0-1
  []
  (?<- (stdout)
       [!column-one !column-two]
       (source-data !column-one !column-two)))

; (query-0-1)

; There are two other variable types in Cascalog and I'll introduce non-null
; variables which start with a question mark '?'.
(defn query-0-2
  []
  (?<- (stdout)
       [?column-one !column-two]
       (source-data ?column-one !column-two)))

; (query-0-2)

; Executing the above gives:
;   RESULTS
;   -----------------------
;   _dAtA wITh_   _twO cOlumns_
;   _some rows_   null
;   _And it AlSo_ _hAs four rows_
;   -----------------------
;
; as ?column-one cannot be nil but !column-two can.

(defn query-0-3
  []
  (?<- (stdout)
       [!column-one ?column-two]
       (source-data !column-one ?column-two)))

; (query-0-3)

; Executing the above gives:
;   RESULTS
;   -----------------------
;   _dAtA wITh_   _twO cOlumns_
;   null          _contain nils_
;   _And it AlSo_ _hAs four rows_
;   -----------------------
;
; as now ?column-two cannot but !column-one can.

; -----------------------
; Defining queries as functions

(defn source-tap-with-sparkles
  "Using the <- marco we can define a source tap which might contain some data
  clean up before we return it to the user."
  []
  ; Create a query with <- and name the two variables we wish to return, just
  ; as before.
  (<- [?column-one ?column-two]

      ; This time when we read from our source stream the rows are not being
      ; assigned to the returned variable but intermediate ones.
      (source-data ?c-one ?c-two)

      ; Now we don't want all the silly casing and instead need column one in
      ; all lower case and column two as upper case.
      ; Here I introduce two new symbols although only one is required.
      ;   :< defines the list of input variables to the function, everything
      ;      following it is given as an input to the funtion; and
      ;   :> defines the list of output variables assigned the output of the
      ;      function.
      ;
      ; Ymay notice I've been assigning data to variables without the use of
      ; :> before. Cascalog is fairly intelligent and if your function takes no
      ; arguments it assumes everything is output, as has been the case so far.
      ; Otherwise it assumes everything is an input until it comes across the
      ; :> macro. The :< are rarely required and I almost never use them. I do
      ; tend to be explicit about :> so the above is not my normal coding style.
      ; I don't know what the canonical is probably to use the implicit useage
      ; where possible. Who likes typing ;)
      (s/lower-case :< ?c-one :> ?column-one)
      (s/upper-case :< ?c-two :> ?column-two)))

(defn query-1
  []
  (?<- (stdout)
       [?column-one ?column-two]
       ((source-tap-with-sparkles) :> ?column-one ?column-two)))

(query-1)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;   _data with_    _TWO COLUMNS_
;   _and it also_  _HAS TWO ROWS_
;   -----------------------
;
; and we've used non-nil variables for both as now we're only interested in
; variables with data.

; This we can unit test... jumping to core_test.clj... and we're back.


(defn -main
  "The project's main function."
  [& args]
  (println "Hello cam-cli!"))
