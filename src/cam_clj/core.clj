(ns cam-clj.core
  (:require [cascalog.api :refer :all]
            [cascalog.more-taps :refer [hfs-delimited]]
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

; (query-1)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;   _data with_    _TWO COLUMNS_
;   _and it also_  _HAS TWO ROWS_
;   -----------------------
;
; and we've used non-nil variables for both as now we're only interested in
; variables with data.

; This we can unit test... jumping to core_test.clj (assuming I ever get them
; to work)... and we're back.

; -----------------------
; The MovieLens datasets
;
; For this we're going create source taps for three of the MoveLens data sets.
; From the README in the MovieLen data set zip:
;  u.data: The full u data set, 100000 ratings by 943 users on 1682 items. This
;    is a tab separated list of:
;      user id | item id | rating | timestamp
;    The time stamps are unix seconds since 1/1/1970 UTC.
;  u.item Information about the items (movies); this is a pipe separated list of:
;      movie id | movie title | release date | video release date | IMDB URL
;      | unknown | Action | Adventure | Animation | Children's | Comedy | Crime
;      | Documentary | Drama | Fantasy | Film-Noir | Horror | Musical | Mystery
;      | Romance | Sci-Fi | Thriller | War | Western
;    The last 19 fields are the genres, a 1 indicates the movie is of that
;    genre, a 0 indicates it is not; movies can be in several genres at once.
;    The movie ids are the ones used in the u.data data set.
;
; So lets create some taps.

(defn user-data
  "Returns a source tap for the user data set user ID and item ID columns.

  Takes the path to load the data from."
  [src]
  (let [; Here we define, as strings, the variable names for the columns and
        ; the corresponding types that they should be represented as.
        ; The data structure is my own devising, it's just to ensure they are
        ; in the *correct order for the file* and the field is obviously
        ; associated with it's type.
        all-columns [["!user-id" Long]
                     ["!item-id" Long]
                     ["!rating" Long]
                     ["!timestamp-raw" Long]]

        ; Getting the fields and types is straight forward. I've also used an
        ; array-map before and then you ask for keys and vals. Can't decide
        ; which I prefer...
        fields (map first all-columns)
        classes (map second all-columns)

        ; We are only interested in three of the columns so lets just select
        ; those.
        returned-columns ["!user-id" "!item-id" "!rating"]

        ; A new function :)
        ; hfs-delimited is used to read delimited files off the HDFS file
        ; system however it can handle the local file system too so I tend
        ; to just use this as typically the LFS is just used for small tests.
        ; There is an lfs-delimited that takes the same options.
        input-tap (hfs-delimited src ;; Input path, today only file:// but more
                                     ;; typically hdfs:// or s3://
                                 :delimiter "\t"
                                 :outfields fields
                                 :classes classes

                                 ; Often delimited files have a header row with
                                 ; the field names and it should be skipped (it
                                 ; can't name the fields :(). Default is false
                                 ; just highlighting it exists.
                                 :skip-header? false)]

    ; Now we return the fields that we want using select-fields. Note all this
    ; has been done with lists of strings. The best documentation for
    ; select-fields and hdf-delimited is still
    ; https://groups.google.com/forum/#!msg/cascalog-user/t0LsCp3hxiQ/LDlQVAFE8gUJ
    (select-fields input-tap returned-columns)))

; This is where the data lives, this just demos creating the object.
; (user-data "resources/data/ml-100k/u.data")

; Now lets load the user item ratings which stores booleans as longs. Not
; really ideal so lets convert them.
; TODO Introduce defmapfn
(defn long-to-bool
  "Converts zero to false and one to true. Gets unhappy otherwise."
  [& args]
  (map #(case %
         0 false
         1 true
         nil) args))

(defn user-item
  "Retuns a source tap for the user item ratings. Takes a path to the dataset."
  [src]
  (let [; Told you I've experimented with array-map. The order is important
        ; as it *must* match the TSV column ordering. Less brackets with this
        ; method.
        all-columns (array-map "!movie-id" Long
                               "!movie-title" String
                               "!release-date" String
                               "!video-release-date" String
                               "!imdb-url" String
                               "!unknown" String ;; really?!
                               "!action-raw" Long
                               "!adventure-raw" Long
                               "!animation-raw" Long
                               "!childrens-raw" Long
                               "!comedy-raw" Long
                               "!crime-raw" Long
                               "!documentary-raw" Long
                               "!drama-raw" Long
                               "!fantasy-raw" Long
                               "!film-noir-raw" Long
                               "!horror-raw" Long
                               "!musical-raw" Long
                               "!mystery-raw" Long
                               "!romance-raw" Long
                               "!sci-fi-raw" Long
                               "!thriller-raw" Long
                               "!war-raw" Long
                               "!western-raw" Long)
        fields (keys all-columns)
        classes (vals all-columns)

        ; These are the fields we wish to convert to booleans and their final
        ; names.
        fields-to-bool (array-map "!action-raw" "!action"
                                  "!adventure-raw" "!adventure"
                                  "!animation-raw" "!animation"
                                  "!childrens-raw" "!childrens"
                                  "!comedy-raw" "!comedy"
                                  "!crime-raw" "!crime"
                                  "!documentary-raw" "!documentary"
                                  "!drama-raw" "!drama"
                                  "!fantasy-raw" "!fantasy"
                                  "!film-noir-raw" "!film-noir"
                                  "!horror-raw" "!horror"
                                  "!musical-raw" "!musical"
                                  "!mystery-raw" "!mystery"
                                  "!romance-raw" "!romance"
                                  "!sci-fi-raw" "!sci-fi"
                                  "!thriller-raw" "!thriller"
                                  "!war-raw" "!war"
                                  "!western-raw" "!western")]
    (<- ; I can still use the symbol form even when dealing with strings.
        ; Note after all that bool work we don't want them!
        [!movie-id !movie-title]

        ; There are :<< and :<< marcos which mimic the :< and :> marcos but
        ; instead of dealing with explicit symbols they deal with lists of
        ; string. This is useful for dealing with a large number of fields,
        ; especially when you want to keep them all. select-fields is only
        ; useful when you want to reduce the number.
        ((hfs-delimited src
                        :delimiter "\t"
                        :outfields fields
                        :classes classes) :>> fields)

        ; Here the :<< is required as it causes Cascalog to unpack the input
        ; list and passes the variable values rather than passing a list of
        ; strings. It's one of the cases where you need to be explicit about
        ; the input operator. As normal you must be explicit about where the
        ; output starts.
        (long-to-bool :<< (keys fields-to-bool)
                      :>> (vals fields-to-bool)))))

; This is where the data lives, this just demos creating the object.
; (user-item "resources/data/ml-100k/u.data")


(defn -main
  "The project's main function."
  [& args]
  (println "Hello cam-cli!"))
