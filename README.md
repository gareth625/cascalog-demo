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

[Cascading](http://www.cascading.org) is a data processing API and query planner API.[1](http://docs.cascading.org/cascading/2.5/userguide/htmlsingle/#N20050) You write queries that Cascalog then expresses as Cascading queries which can then be run on you laptop in a local mode or on a Apache Hadoop platform, such as Amazon's [Elastic Mapreduce](http://aws.amazon.com/elasticmapreduce/) which we use at [Metail](www.metail.com) however there are plenty of platforms available.

Cascading itself provides an API for developing mapreduce algorithms in a much more natural way than when using the Hadoop API. At least that's what I've been told, I've not yet had to do that myself ;) There are also plenty of DSL for Cascading e.g. [Scalding](https://github.com/twitter/scalding) for [Scala](http://www.scala-lang.org). Obviously Cascalog is the best of these ;)

## Setting Up Your Environment

If you're new to Clojure, check out the [Getting Started Guide](http://dev.clojure.org/display/doc/Getting+Started). It would be useful to setup your development environment and install [Leiningen](http://leiningen.org/) before you come along.

Clone (or fork) this git repository, and run `lein deps` to pull in some useful libraries and test data.

## The MovieLens data set

This repository works with a dataset of users and film ratings from MovieLens, see [MovieLens Data Sets](http://www.grouplens.org/node/73) for download links. The MovieLens 100k dataset needs to be downloaded an extracted on your local disk:

    mkdir -p resources/data
    cd resources/data
    wget http://www.grouplens.org/system/files/ml-100k.zip
    unzip ml-100k.zip

where I've assumed you are in the root directory of the casclog-demo project. I'll refer to _resources/data/ml-100k_ through the demo although you should use the path you downloaded and extracted the data to.

At this point I will switch to src/cam_clj/core.clj and work through examples that should introduce Cascalog. After that, I'll return here for some final comments.

## Error Handling and Debugging

This is not covered in this presentation, despite it being the most important thing (along with (unit) testing) for project development. This is mostly because it's hard to understand how to handle errors without understanding the basic syntaxs of the DSL. The minor part of why I didn't cover it is because it is more normal to demo things that work rather than fail. If we have time I can demo some of the error handling we've been learning at Metail. [Here](https://github.com/nathanmarz/cascalog/wiki/Troubleshooting,-testing-and-live-coding) is a basic introduction to error handling with traps.

### :traps

Traps are a Cascading [concept](http://docs.cascading.org/cascading/2.0/userguide/htmlsingle/#N21366) and are roughly equivalent to a catch all statement with a caveat I don't full understand (lack of robust unit tests to flesh out the behaviour!). The documentation on them discourages their use except as a last ditch way of preventing your job crashing (e.g. catch all) when an unexpected error occurs. For known errors you should handle the errors and attempt to complete the processing of that chain. The caveat seems to be that they only operate for errors thrown within the current generator scope or functions called by that generator but not by generators called by that generate (or perhaps another level down). It's hard to explain and I don't really understand it myself, however it appears placing a single :trap at the point where you execute your top level query won't help if something does something unexpected a few function calls down. Personally I have a sneaking suspicious it's the problem of this [person](https://groups.google.com/d/msg/cascalog-user/-SV7-AAuBTo/7a2NQPSAfEsJ).

At the moment our jobs are limping along without any decent error logging which I think would involve being able to switch the sink tap you write to either through exception handling, if/else branches, multimethods, etc. One of the tools we use to manage our web analytics, [Snowplow](http://snowplowanalytics.com) has a concept of events, successfully processed data; bad rows, data you can't process but know why; and error rows which are errors that caught you off guard. In Cascalog the traps can be used for the latter and in Metail we have a well defined concept of events and error rows, and our known bad rows are either silently ignored or finding their way into stdout while being ignored! Hence I can't give many pointers on this part.

## Other Useful Tools

These are some of the tools that we're using regularly in Metail to run our Cascalog jobs on a Hadoop cluster plus a few that actively being investigated and we hope will make our lives easier:

  * [Elastic Mapreduce](http://aws.amazon.com/elasticmapreduce/): This is an Hadoop cluster built atop of Amazon's [Elastic Compute Cloud (EC2)](http://aws.amazon.com/ec2/). Amazon are using their EC2 infrastructure to provide Hadoop as a service. This is what we are using in Metail, others are available but I've not personally tried them;
  * [Lemur](https://github.com/TheClimateCorporation/lemur) is a tool that allows you to launch Hadoop jobs locally or on EMR[1](https://github.com/TheClimateCorporation/lemur) and the definition of your EMR job is done in Clojure. We're using this heavily in Metail and it is very powerful. It has two main draw backs, the first is that the current release doesn't support VPC subnets and different endpoints, and it is effectively a program that is given a config file albeit one written in Clojure. The use VPC subnets is now the canonical way to configure the network that governs you cluster, and the ability to set an endpoint allows you to run in different regions which is particularly important for us Europeans. Lemur does support VPC subnets and endpoints however this is only since March hence you have to clone the gihub repo and build it from there. There is an issue on the github repo to allow Lemur to be included as a library from within your own project. This would mean you only need Java installed to launch your jobs and not Lemur itself.
  * [Amazonica](https://github.com/mcohen01/amazonica) is a compreshensive Clojure library for the Amazon AWS API which we are yet to try. It can be used as a Clojure library solving at least one of Lemurs problems, however I've never tried so I can't compare the two; and
  * [Incanter](http://incanter.org) which Metail never really got into and instead use [R](http://www.r-project.org). Incanter plots look a bit rubbish and the library was a bit lacking. Incidentally R's default plots look a bit rubbish too but it does have some good plotting libraries. Incanter may now, I've not looked in a year.

## Conclusions

Hopefully by this point having read this README and core.clj you've a reasonable understanding of the Cascalog DSL, how to use functions to transform individual variables, how to aggregate over the queries in a data set and some of the key concepts required to write Cascalog workflows. Thank you for listening (or reading).
