(ns cam-clj.core
  (:require [cascalog.api :refer :all]
            [cascalog.logic.ops :as ops]
            [cascalog.more-taps :refer [hfs-delimited]]
            [clojure.data.priority-map :refer [priority-map priority-map-by]]
            [clojure.set :as set]
            [clojure.string :as s]
            [incanter.core :refer [abs]]
            [incanter.stats :refer [correlation euclidean-distance]])
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

   ; The <- macro is used to define a generator, where a generator is a set of
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
; as you might guess, defines both defines and executes the generator i.e. it's
; a combination of the ?- and <- macros. It's slight less verbose.
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
; So lets create some taps. The first is from the user data.
(def user-data-path "resources/data/ml-100k/u.data")

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
                     ["!movie-id" Long]
                     ["!rating" Long]
                     ["!timestamp" Long]]

        ; Getting the fields and types is straight forward. I've also used an
        ; array-map before and then you ask for keys and vals. Can't decide
        ; which I prefer...
        fields (map first all-columns)
        classes (map second all-columns)

        ; We are only interested in three of the columns so lets just select
        ; those.
        returned-columns ["!user-id" "!movie-id" "!rating"]

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
    (<- returned-columns
        ((select-fields input-tap returned-columns) :>> returned-columns))))

; Return the data for a single user, just as an example.
(deffilterfn select-user
  "A Cascalog filter function that returns true if the given user ID matches
   a given ID number.

   If a Cascalog filter returns true then the event is kept, if it returns
   false then the event is skipped."
  [required-id user-id]

  ; This is a really trivial function and it is possible to use it inline
  ; rather than wrapping it in a deffilterfn. Cascalog will do the expected
  ; thing with standard Clojure functions.
  (= user-id required-id))

(defn query-2
  []
  (?<- (stdout)
       [!user-id !movie-id !rating]

       ; Use the filter function to select users with ID 196. This could have
       ; been written inline as:
       ;   (= 196 !user-id)
       ; and as there is no output it is used a filter i.e.
       ;   ((= 196 !user-id) :> ?the-user-id)
       ; will not filter as the output is made available.
       ;
       ; Note it could not have been written:
       ;   ((partial = 196) !user-id)
       ; one because it's stupid but more importantly because Cascalog passes
       ; the names of functions and variables hence it can't use anonamous
       ; functions as they have no name!
       (select-user 196 !user-id)
       ((user-data user-data-path) !user-id !movie-id !rating)))

;  (query-2)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;   196 242  3
;   196 393  4
;   196 381  4
;   196 251  3
;   196 655  5
;   196 67   5
;   196 306  4
;   196 238	 4
;   196 663	 5
;   196 111	 4
;   196 580	 2
;   196 25	 4
;   196 286	 5
;   196 94	 3
;   196 692	 5
;   196 8    5
;   196 428  4
;   196 1118 4
;   196 70   3
;   196 66   3
;   196 257  2
;   196 108  4
;   196 202  3
;   196 340  3
;   196 287  3
;   196 116  3
;   196 382  4
;   196 285  5
;   196 1241 3
;   196 1007 4
;   196 411  4
;   196 153  5
;   196 13   2
;   196 762  3
;   196 173  2
;   196 1022 4
;   196 845  4
;   196 269  3
;   196 110  1
;   -----------------------
;
; and we've used non-nil variables for both as now we're only interested in
; variables with data.

(defn query-2-1
  []
  (?<- (stdout)

       ; In this case perhaps a more sensible way of filter is to specific a
       ; constant for the user ID rather than a variable. In the end why return the
       ; ID when we know it's 196 :)
       [!movie-id !rating]
       ((user-data user-data-path) 196 !movie-id !rating)))

; This gives the same output as query-2.
; (query-2-1)

; Now a tap for the user items dataset.
(def user-item-path "resources/data/ml-100k/u.item")

; Cascalog has two mapper functions, one takes a set of arguments and returns a
; single tuple with the new variables and is used to add new fields to an event.
; The other is used to create new events by returning a sequence of tuples.
; Here I'm demonstrating the former using defmapfn. When we load the user item
; ratings the booleans are stored as longs which is not ideal so lets convert
; them.
(defmapfn long-to-bool
  "Converts zero to false and one to true. Gets unhappy otherwise."
  [& args]
  (map #(case %
         0 false
         1 true
         nil) args))

(defn user-item-bools
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
        [!movie-id !movie-title !action !adventure !animation !childrens
         !comedy !crime !documentary !drama !fantasy !film-noir !horror
         !musical !mystery !romance !sci-fi !thriller !war !western]

        ; There are :<< and :<< marcos which mimic the :< and :> marcos but
        ; instead of dealing with explicit symbols they deal with lists of
        ; string. This is useful for dealing with a large number of fields,
        ; especially when you want to keep them all. select-fields is only
        ; useful when you want to reduce the number.
        ((hfs-delimited src
                        :delimiter "|"
                        :outfields fields
                        :classes classes) :>> fields)

        ; Here the :<< is required as it causes Cascalog to unpack the input
        ; list and passes the variable values rather than passing a list of
        ; strings. It's one of the cases where you need to be explicit about
        ; the input operator. As normal you must be explicit about where the
        ; output starts.
        (long-to-bool :<< (keys fields-to-bool)
                      :>> (vals fields-to-bool)))))

(defn query-3
  []
  (let [fields [1 ; Neat filtering on the movie ID in our field list.
                "!movie-title"
                "!action"
                "!adventure"
                "!animation"
                "!childrens"
                "!comedy"
                "!crime"
                "!documentary"
                "!drama"
                "!fantasy"
                "!film-noir"
                "!horror"
                "!musical"
                "!mystery"
                "!romance"
                "!sci-fi"
                "!thriller"
                "!war"
                "!western"]]
    (?<- (stdout)
         [!movie-title !action "!childrens"]
         ((user-item-bools user-item-path) :>> fields))))

; (query-3)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;   Toy Story (1995) false true
;   -----------------------
;
; Turns out Toy Story isn't an action movie, it's a childrens movie!

(defn user-item
  "Retuns a source tap for the user item ratings returing the movie ID and title."
  [src]
  (let [all-columns (array-map "!movie-id" Long
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
        classes (vals all-columns)]
    (<- [!movie-id !movie-title]
        ((hfs-delimited src
                        :delimiter "|"
                        :outfields fields
                        :classes classes) :>> fields))))

(defn query-3-1
  []
  (let [fields [1 ; Neat filtering on the movie ID in our field list.
                "!movie-title"]]
    (?<- (stdout)
         [!movie-title]
         ((user-item user-item-path) :>> fields))))

; (query-3-1)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;   Toy Story (1995)
;   -----------------------
;
; As we're not interested in the genre for this exercise!


; Now we need to combine these two independent sources with a common field
; together into one data set.
(defn user-ratings
  "Returns a generator over the user item and data. Give the User's rating for
   each movie."
  [u-data-path u-item-path]
  (<- [!user-id !movie-id !movie-title !rating]

      ; Cascalog will automatically perform an SQL like inner join across
      ; generators. You just need to specify the common fields to join across
      ; which in this case is the movie ID. Now we return the merged data sets.
      ((user-data u-data-path) !user-id !movie-id !rating)
      ((user-item u-item-path) !movie-id !movie-title)))

(defn query-4
  []
  (?<- (stdout)

       ; I don't want any null entries in this query. Using the ? variables to
       ; filter them out.
       [?movie-id ?movie-title ?rating]
       ((user-ratings user-data-path user-item-path) 196 ?movie-id ?movie-title ?rating)))

; (query-4)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;   8    Babe (1995)                                                         5
;   13   Mighty Aphrodite (1995)                                             2
;   25   Birdcage, The (1996)                                                4
;   66   While You Were Sleeping (1995)                                      3
;   67   Ace Ventura: Pet Detective (1994)                                   5
;   70   Four Weddings and a Funeral (1994)                                  3
;   94   Home Alone (1990)                                                   3
;   108  Kids in the Hall: Brain Candy (1996)                                4
;   110  Operation Dumbo Drop (1995)                                         1
;   111  Truth About Cats & Dogs, The (1996)                                 4
;   116  Cold Comfort Farm (1995)                                            3
;   153  Fish Called Wanda, A (1988)                                         5
;   173  Princess Bride, The (1987)                                          2
;   202  Groundhog Day (1993)                                                3
;   238  Raising Arizona (1987)                                              4
;   242  Kolya (1996)                                                        3
;   251  Shall We Dance? (1996)                                              3
;   257  Men in Black (1997)                                                 2
;   269  Full Monty, The (1997)                                              3
;   285  Secrets & Lies (1996)                                               5
;   286  English Patient, The (1996)                                         5
;   287  Marvin's Room (1996)                                                3
;   306  Mrs. Brown (Her Majesty, Mrs. Brown) (1997)                         4
;   340  Boogie Nights (1997)                                                3
;   381  Muriel's Wedding (1994)                                             4
;   382  Adventures of Priscilla, Queen of the Desert, The (1994)            4
;   393  Mrs. Doubtfire (1993)                                               4
;   411  Nutty Professor, The (1996)                                         4
;   428  Harold and Maude (1971)                                             4
;   580  Englishman Who Went Up a Hill, But Came Down a Mountain, The (1995) 2
;   655  Stand by Me (1986)                                                  5
;   663  Being There (1979)                                                  5
;   692  American President, The (1995)                                      5
;   762  Beautiful Girls (1996)                                              3
;   845  That Thing You Do! (1996)                                           4
;   1007 Waiting for Guffman (1996)                                          4
;   1022 Fast, Cheap & Out of Control (1997)                                 4
;   1118 Up in Smoke (1978)                                                  4
;   1241 Van, The (1996)                                                     3
;   -----------------------
;
; So now we have all the movies user 196 rated.

; At this point it is worth mentioning that Cascalog's third variable type
; starts with a leading !! e.g. !!ratings and is used to indicating that an
; outer rather than inner join should be used to join generators. I'm a bit
; short of (writing) time so I may or may not come back to here and demo it
; with a trivial example.

; -----------------------
; The Recommender
;
; I've been a tad lazy, or perhaps helpful, by simply reimplementing the
; example recommender built for one of the Meetup's coding dojos. This will,
; perhaps, allow you to have a comparison to something done before in the Dojo.
; The original can be found here:
; https://github.com/cam-clj/Recommendations/blob/example-solution/.

; As the first step we need a query that returns the similarity between pairs
; of users. The original recommender used a nested map structure i.e. a map
; with the user ID as the key and value of a map linking the user's rated movie
; ID to it's rating.
;
; In Cascalog we pass around tuples containing columns when dealing with mapper
; functions. In the above example it's equivalent to the map containing movie
; ID and rating, in this case 2-tuples are passed to functions defined by e.g.
; the defmapfn or deffilterfn macros. Of course Cascalog has a macro for
; functions that run as reducers and these are given all the 2-tuples (more
; generally n-tuples) in a sequence. In order to define aggregation functions
; we can use the deffilterfn macro.

; Calculating the similarity between two users.
; This code has been, pretty much, directly taken from the original
; recommender. I've had to make a small change as we're dealing with different
; data structures. It's just pure Clojure and has actually become a biit clunky
; now as I haven't been able to map it's general nature to Cascalog well! Still
; I'd like to stick closely to the original recommender code and leave it as an
; exercise to the (more experienced) reader e.g. future Me :)
(defn build-sim-fn
  "Given a function `f` to score two sets of ratings for similarity,
  return a function that takes a map of ratings and two people, and
  returns their similarity score."
  [f]
  (fn [ratings]
    (if ((comp seq first) ratings)
      (f (map (fn [[_ r1 _]] r1) ratings) (map (fn [[_ _ r2]] r2) ratings))
      nil)))

; A similarity function based on Euclidean distance
(def sim-euclidean (build-sim-fn #(/ 1 (+ 1 (euclidean-distance %1 %2)))))

; A similarity function based on Pearson correlation. We scale the correlation
; (which is between -1 and 1) to give a value between 0 and 1, so it's on the
; same scale as sim-euclidean.
(def sim-pearson (build-sim-fn #(/ (+ 1 (correlation %1 %2)) 2)))

; Now we define our Cascalog aggregator which take a 1-tuple containing
; 3-tuples i.e.
;   [[movie-id-1 user-1-rating-1 user-2-rating-1]
;    [movie-id-2 user-1-rating-2 user-2-rating-2]
;    ...
;    [movie-id-n user-1-rating-n user-2-rating-n]]
(defbufferfn sim-euclidean-buffer
  "Returns the euclidean similarity for a pair of users."
  [ratings]

  ; Note that I'm returning a tuple. Aggregators as expected to return a
  ; sequence of n-tuples which are unpacked to output variables. Here I'm
  ; returning a 1-tuple of the euclidean similarity of two users. It can be
  ; nil.
  [(sim-euclidean ratings)])

; You can see with my names I've ended up making things a bit clunky. Just wait
; until the next version ;) It would help a bit if this wasn't all in one file!
(defbufferfn sim-pearson-buffer
  "Returns the pearson similarity for a pair of users."
  [ratings]
  [(sim-pearson ratings)])

; The original recommender has a common ratings function which I've
; reimplemented as a Cascalog generator.
(defn common-ratings
  "Returns the set of ratings that all each pair of users has in common from
   the list of ratings."
  [ratings]
  (<- [?user-one ?user-two ?movie-id ?rating-one ?rating-two]

      ; Here we create two generators which output the same ratings datasets
      ; (note I'm providing user-ratings but it could equally be user-data
      ; due to the user of select-fields) and by performing an inner join on
      ; the movie ID we get a new dataset where each row contains user one,
      ; user two and their ratings for that movie i.e. we have access to the
      ; 5-tuple [movie-id user-one user-two rating-one rating-two].
      ((select-fields ratings ["!user-id" "!movie-id" "!rating"]) ?user-one ?movie-id ?rating-one)
      ((select-fields ratings ["!user-id" "!movie-id" "!rating"]) ?user-two ?movie-id ?rating-two)))

(defn similarity
  "Returns the set of movies that two users have in common from the ratings
   source tap. The similarity function to use must be provided."
  [similarity-fn ratings]
  (<- [?user-one ?user-two !similarity]
      ((common-ratings ratings) ?user-one ?user-two ?movie-id ?rating-one ?rating-two)

      ; Now the similarity function we want to use has been passed into the
      ; function so we can call that and send the output to a new !similarity
      ; variable. I sneakily changed the definition of our ratings function to
      ; return nil (original 0) if two users have no movies in common. This
      ; gives the caller the ability to filter them out using a ? variable
      ; rather than a explicit not= 0 filter.
      ;
      ; The aggregator gets given a 1-tuple of the form:
      ;   [[movie-id-1 rating-one-1 rating-two-1]
      ;    [movie-id-2 rating-one-2 rating-two-2]
      ;    ...
      ;    [movie-id-n rating-one-n rating-two-n]]
      ; where the trailing -# indicates the row number. What about the user
      ; IDs, how can we tell who we're rating?! I suspect if you're more
      ; familiar with map reduce it's more obvious but I tried to learn map
      ; reduce through Cascalog so got confused with this bit :)
      ;
      ; We have acccess to the 5-tuple described above so when we call our
      ; aggregator with only a subset of that tuple, in this case 3 variables,
      ; each aggregator call gets given all the rows of those three variables
      ; grouped by the two not given. This allows us to create a new dataset
      ; of [?user-one ?user-two !similarity] with rows for all the unique
      ; combinations of user one and two.
      (similarity-fn ?movie-id ?rating-one ?rating-two :> !similarity)))

; While the above is wonderful and returns all the data it takes a long time to
; run on this laptop. I've no idea how many mappers nor reducers you get local
; mode in Cascalog (well Cascading). In a proper hadoop setup it would be
; speedy. Anyway, in order to demo this within my talk I've created a version
; that only joins across two different users.
(defn common-ratings-quick
  "Returns the set of ratings that a specific pair of users have in common from
   the list of ratings."
  [user-one user-two ratings]
  (<- [?user-one ?user-two ?movie-id ?rating-one ?rating-two]

      ; Here, as user-one and user-two are constants, we're filtering the
      ; datasets before the join. This massively reduces the amount of
      ; computation required of my poor little laptop.
      ((select-fields ratings ["!user-id" "!movie-id" "!rating"])
       :> user-one ?movie-id ?rating-one)
      ((select-fields ratings ["!user-id" "!movie-id" "!rating"])
       :> user-two ?movie-id ?rating-two)

      ; A naughty little way of converting values into Cascalog variables.
      (identity user-one :> ?user-one)
      (identity user-two :> ?user-two)))

(defn similarity-quick
  "Returns the set of movies that a pair of users have in common from the
   ratings source tap. The similarity function to use must be provided."
  [user-one user-two similarity-fn ratings]
  (<- [?user-one ?user-two !similarity]
      ((common-ratings-quick user-one user-two ratings)
       :> ?user-one ?user-two ?movie-id ?rating-one ?rating-two)

      ; None of this is good coding style it's just some trickery to have this
      ; reduce the size of our two datasets down to just the movies rated for
      ; two users but still return in a form that is compatible with our, more
      ; sensible, similarity generator.

      ; This is now only called once as there is only one unique combination of
      ; user-one-id and user-two-id.
      (similarity-fn ?movie-id ?rating-one ?rating-two :> !similarity)))

(defn query-5
  []
  (let [ratings (user-ratings user-data-path user-item-path)]
    (?<- (stdout)
         [?user-one-id ?user-two-id ?similarity-eu ?similarity-pe]

         ; Just pick two users to reduce computation time and save flooding
         ; the console.
         ((similarity-quick 196 186 sim-euclidean-buffer ratings)
          :> ?user-one-id ?user-two-id ?similarity-eu)
         ((similarity-quick 196 186 sim-pearson-buffer ratings)
          :> ?user-one-id ?user-two-id ?similarity-pe))))

; (query-5)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;	  196	186	0.2612038749637414	0.5
;   -----------------------

(defn query-5-1
  "This returns the same output as query-5 but runs the full inner join.

   Takes a while if running in local mode."
  []
  (let [ratings (user-ratings user-data-path user-item-path)]
    (?<- (stdout)
         [?user-one ?user-two ?similarity-eu ?similarity-pe]

         ((similarity sim-euclidean-buffer ratings)
          :> ?user-one ?user-two ?similarity-eu)
         ((similarity sim-pearson-buffer ratings)
          :> ?user-one ?user-two ?similarity-pe)

         ; Now we filter on the two user IDs but have done the join twice and
         ; that takes a while. I could structure this better so you only do the
         ; once but I'm not sure you'd actually run a recommender like that.
         ; This is just an example showing the two different values.
         (= 196 ?user-one)
         (= 186 ?user-two))))

; I'm really slow, you've been warned!
; (query-5-1)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;	  196	186	0.2612038749637414	0.5
;   -----------------------
;
; which is the same as query-5 as expected :)

; After all that work to reduce the number down with the *-quick functions,
; I've realised that I need a data set with four users in it for
; experimentation.
(defn filter-user-ids
  [user-id]

  ; The first four IDs I could see in u.data.
  (#{196 186 22 244} user-id))

(defn user-ratings-reduced
  "Returns a generator over the a subset of the user item and data. Give the
   User's rating for each movie."
  [u-data-path u-item-path]
  (<- [!user-id !movie-id !movie-title !rating]
      ((user-ratings u-data-path u-item-path) !user-id !movie-id !movie-title !rating)
      (filter-user-ids !user-id)))

(defn query-6
 []
 (?<- (stdout)
      [!user-id !movie-id !movie-title !rating]
      ((user-ratings-reduced user-data-path user-item-path)
       :> !user-id !movie-id !movie-title !rating)))

; (query-6)

; Executing the above query gives us:
;   RESULTS
;   -----------------------
;   244	1	Toy Story (1995)	4
;   22	2	GoldenEye (1995)	2
;   244	3	Four Rooms (1995)	5
;   22	4	Get Shorty (1995)	5
;   244	7	Twelve Monkeys (1995)	4
;   196	8	Babe (1995)	5
;   244	9	Dead Man Walking (1995)	5
;   186	12	Usual Suspects, The (1995)	1
;   244	13	Mighty Aphrodite (1995)	4
;   ... many lines ...
;   186	1385	Roseanna's Grave (For Roseanna) (1997)	2
;   186	1399	Stranger in the House (1997)	2
;   244	1428	SubUrbia (1997)	4
;   244	1467	Saint of Fort Washington, The (1993)	5

; Now we need the top n similar users for each user.
(defn top-n-similar-users
  "This takes a tuple of 2-tuples where each tuple will contain the ID of the
   similar user and the similarity to that user. The top n similar users are
   then returned."
  []
  )

(defn top-n-similar-users
  [n ratings]
  (let [similarity-tap (similarity sim-euclidean-buffer ratings)]
    (<- [?user-one ?user-two-top ?similarity-top]

        ((similarity sim-euclidean-buffer ratings) ?user-one ?user-two ?similarity)

        ((ops/limit n) ?similarity ?user-two :> ?similarity-top ?user-two-top))))

(defn query-7
  []
  (let [n 2
        ratings (user-ratings-reduced user-data-path user-item-path)]
    (?<- (stdout)
         [?user-one ?user-two ?similarity]
         ((top-n-similar-users n ratings) ?user-one ?user-two ?similarity))))

(query-7)

; If we were to just ask for the similiarity for all users we would get:
;   RESULTS
;   -----------------------
;   22	22  1.0                 0.9999999999999998
;   22	186 0.10815240673485554 0.5293321664978604
;   22	196 0.16952084719853724 0.6790223608430471
;   22	244 0.07595595279317306 0.5917112513515389
;   186	22  0.10815240673485554 0.5293321664978604
;   186	186 1.0                 0.9999999999999998
;   186	196 0.2612038749637414  0.5
;   186	244 0.09535767393825997 0.41382063624711934
;   196	22  0.16952084719853724 0.6790223608430471
;   196	186 0.2612038749637414  0.5
;   196	196 1.0                 1.0000000000000002
;   196	244 0.20521309615767264 0.636545346920903
;   244	22  0.07595595279317306 0.5917112513515388
;   244	186 0.09535767393825997 0.4138206362471193
;   244	196 0.20521309615767264 0.636545346920903
;   244	244 1.0                 1.0000000000000004
;   -----------------------
;
; However, executing the above query gives us:
;   RESULTS
;   -----------------------
;   22	22	1.0
;   22	186	0.10815240673485554
;   186	22	0.10815240673485554
;   186	186	1.0
;   196	22	0.16952084719853724
;   196	186	0.2612038749637414
;   244	22	0.07595595279317306
;   244	186	0.09535767393825997
;   -----------------------

; We use a priority map to accumulate the top n users. This is a map sorted on
; value, so the first element of the map (returned by `peek`) has the smallest
; value, and `pop` removes this entry from the map. Once the accumulator has
; grown to `n` entries, we compare the next score with the first entry and, if
; it is bigger, pop off the smaller entry and add the new one. Otherwise, we
; ignore the new entry and return the accumulator unchanged.

(defn top-n-similar-users
  "Find the `n` users most similar to `p`, where the similarity between
  two users is computed by `sim-fn`. Return a map keyed by user id
  whose value is the similarity score for that user."
  [sim-fn ratings p n]
  (reduce (fn [accum p']
            (let [s (sim-fn ratings p p')]
              (cond
               (< (count accum) n)      (conj accum [p' s])
               (> s (val (peek accum))) (conj (pop accum) [p' s])
               :else                    accum)))
          (priority-map)
          (remove #{p} (keys ratings))))

(defn score-for
  "Given a map of friends' similarity scores, return the weighted score for `item`."
  [ratings friends item]
  (loop [n 0 d 1 friends (seq friends)]
    (if friends
      (let [[friend-id friend-similarity] (first friends)]
        (if-let [friend-rating (get-in ratings [friend-id item])]
          (recur (+ n (* friend-rating friend-similarity)) (+ d (abs friend-similarity)) (next friends))
          (recur n d (next friends))))
      (/ n d))))

(defn recommendations-for
  "Return a sorted sequence of recommendations for `p`, with the highest recommendation first."
  ([sim-fn ratings p]
     (recommendations-for sim-fn ratings p (count ratings)))
  ([sim-fn ratings p n]
     (let [friends (top-n-similar-users sim-fn ratings p n)
           unseen  (remove (ratings p) (reduce set/union (map (comp set keys ratings) (keys friends))))
           ranked  (into (priority-map-by >)
                         (map vector unseen (map (partial score-for ratings friends) unseen)))]
       (keys ranked))))


; -----------------------
; Main
;
; I'm sure I'll find something to do with this :P
(defn -main
  "The project's main function."
  [& args]
  (println "Hello cam-cli!"))
