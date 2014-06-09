# Cascalog Demonstration For The Cambridge-NonDysFunctional-Programmers Meetup

A demonstration of using [Cascalog](cascalog.org) for the Cambridge-NonDysFunctional-Programmers [Meetup](http://www.meetup.com/Cambridge-NonDysFunctional-Programmers/events/183253772/).

This aim of this project is to provide me with code and pictures to give an, approximaltely, one hour talk at the Cambridge-NonDysFunctional-Programmers Meetup in [June](http://www.meetup.com/Cambridge-NonDysFunctional-Programmers/events/183253772/). The project will go through the implementation of k-means clustering for the [Movie Lens 100k data set](http://www.grouplens.org/node/73). It's an idea I've shameless stolen the idea from a previous NonDysFunctional Programmers [Meetup](https://github.com/cam-clj/Recommendations). However it will provide an excellent introduction to concept of using Cascalog to write filters, mappers and aggregators, as well as providing an opportunity to see some different ways to declare and use variables. Unfortunately it will not provide any guidance on running on Hadoop clusters and certainly not setting them up, still there will be some pointers at the end.

## Cascalog

"[Cascalog](cascalog.org) is a Clojure based query language for Hadoop inspired by [Datalog](http://en.wikipedia.org/wiki/Datalog)"[1](http://nathanmarz.com/blog/introducing-cascalog-a-clojure-based-query-language-for-hado.html).

That is taken from Nathan Marz's blog post where he first annonced the release of Cascalog. What else can I say about [Cascalog](cascalog.org)?

### Midje-Cascalog

It is unit testable. After going through this tutorial you'll, hopefully understand that it is not trivial to write unit tests from scratch for Cascalog. You would need a fair bit of boiler plate code just get your first "Hello World" unit test. Luckily the author's of Cascalog were faced with this problem and they created a unit testing framework based on [Midje](https://github.com/marick/Midje) in the form of [Midje-Cascalog](https://github.com/nathanmarz/cascalog/tree/develop/midje-cascalog).

[Midje](https://github.com/marick/Midje) was created to allow programmers to ["test with ease"](https://github.com/marick/Midje#about-midje). I came across Midje when investigating how to write unit tests for Cascalog which lead me to [Midje-Cascalog](https://github.com/nathanmarz/cascalog/tree/develop/midje-cascalog). Both are conceptually similar however a lot of the Midje concepts have been reimplemented in Midje-Cascalog as they can't quite be used directly. At least as far as I can tell :)

I have found it easy to write unit tests in Midje-Cascalog and I've started exploring the Midje unit test framework instead of Clojure test. However, like all things Cascalog, if something goes wrong i.e. the test fails, then it can be a real pain to debug. To this end I'm not gong to cover Midje-Cascalog in this talk as I couldn't get it to work and didn't have the time to come back to it :( Look in the test directory of this project for my initial attempt.

My advice here, it where possible, don't just test your Cascalog function make sure you test your standard Clojure functions too. Often I've found that when they fail you get an uninformative error message. Just as often I've found those functions are written correctly I'm just giving them poorly formed data and they're failing when called from a Cascalog function ;)

That feels a bit negative, it's not as bad as it sounds. Still not covering unit tests :(

### Cascading

[Cascading](http://www.cascading.org) is a data processing API and query planner API.[1](http://docs.cascading.org/cascading/2.5/userguide/htmlsingle/#N20050) You write queries that Cascalog then expresses as Cascading queries which can then be run on you laptop in a local mode or on a Apache Hadoop platform, such as Amazon's (Elastic Mapreduce)[http://aws.amazon.com/elasticmapreduce/] which we use at [Metail](www.metail.com) however there are plenty of platforms available.

Cascading itself provides an API for developing mapreduce algorithms in a much more natural way than when using the Hadoop API. At least that's what I've been told, I've not yet had to do that myself ;) There are also plenty of DSL for Cascading e.g. [Scalding](https://github.com/twitter/scalding) for [Scala](http://www.scala-lang.org). Obviously Cascalog is the best of these ;)

### Taps

....

### Source Taps -- The Movie Lens Input

Reading from the different type of delimited files, joining across data sets and print the output. Might use a reducer to filter implement take 10 or just a filter to get a specific user and their 20 movies.

Use hfs-delimited and select-keys. Discuss the different file system sources, file vs directory, HDFS vs LFS and a note about S3.

### Sink Taps

stdout, produces and other midje-cascalog sinks, files (HDFS, LFS and S3)

## Setting Up Your Environment

If you're new to Clojure, check out the [Getting Started Guide](http://dev.clojure.org/display/doc/Getting+Started). It would be useful to setup your development environment and install [Leiningen](http://leiningen.org/) before you come along.

Clone (or fork) this git repository, and run `lein deps` to pull in some useful libraries and test data.

### The MovieLens data set

This repository works with a dataset of users and film ratings from MovieLens, see [MovieLens Data Sets](http://www.grouplens.org/node/73) for download links. The MovieLens 100k dataset needs to be downloaded an extracted on your local disk:

    mkdir -p resources/data
    cd resources/data
    wget http://www.grouplens.org/system/files/ml-100k.zip
    unzip ml-100k.zip

where I've assumed you are in the root directory of the casclog-demo project. I'll refer to _resources/data/ml-100k_ through the demo although you should use the path you downloaded and extracted the data to.

## Conclusions

Sum up.
