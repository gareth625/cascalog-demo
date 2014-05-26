# Cascalog Demonstration For The Cambridge-NonDysFunctional-Programmers Meetup

A demonstration of using [Cascalog](cascalog.org) for the Cambridge-NonDysFunctional-Programmers [Meetup](http://www.meetup.com/Cambridge-NonDysFunctional-Programmers/events/183253772/).

This aim of this project is to provide me with code and pictures to give an, approximaltely, one hour talk at the Cambridge-NonDysFunctional-Programmers Meetup in [June](http://www.meetup.com/Cambridge-NonDysFunctional-Programmers/events/183253772/). The project will go through the implementation of k-means clustering for the [Movie Lens 100k data set](http://www.grouplens.org/node/73). It's an idea I've shameless stolen the idea from a previous NonDysFunctional Programmers (Meetup)[https://github.com/cam-clj/Recommendations]. However it will provide an excellent introduction to concept of using Cascalog to write filters, mappers and aggregators, as well as providing an opportunity to see some different ways to declare and use variables. Unfortunately it will not provide any guidance on running on Hadoop clusters and certainly not setting them up, still there will be some pointers at the end.

## Introduction

### Setting Up Your Environment

If you're new to Clojure, check out the [Getting Started Guide](http://dev.clojure.org/display/doc/Getting+Started). It would be useful to setup your development environment and install [Leiningen](http://leiningen.org/) before you come along.

Clone (or fork) this git repository, and run `lein deps` to pull in some useful libraries and test data.

### The MovieLens data set

This repository works with a dataset of users and film ratings from MovieLens, see [MovieLens Data Sets](http://www.grouplens.org/node/73) for download links. The MovieLens 100k dataset needs to be downloaded an extracted on your local disk:

    mkdir -p resources/data
    cd resources/data
    wget http://www.grouplens.org/system/files/ml-100k.zip
    unzip ml-100k.zip

where I've assumed you are in the root directory of the casclog-demo project. I'll refer to _resources/data/ml-100k_ through the demo although you should use the path you downloaded and extracted the data to.

## Cascalog

Intro to Cascalog

### Cascading

Explain Cascading's taps and flow philosophy

### Midje and Midje-Cascalog

Demo some source and sink taps using Midje unit tests

## Taps

### Source Taps -- The Movie Lens Input

Reading from the different type of delimited files, joining across data sets and print the output. Might use a reducer to filter implement take 10 or just a filter to get a specific user and their 20 movies.

Use hfs-delimited and select-keys. Discuss the different file system sources, file vs directory, HDFS vs LFS and a note about S3.

### Sink Taps

stdout, produces and other midje-cascalog sinks, files (HDFS, LFS and S3)

### A Note on :<< and :>>

Dealing with wide sources, actually as easy as it looks when you the & in the right place.

## k-means clustering

Implement a k-means clustering algorithm on our files.

## Incanter and Visualising the output

This is a might be nice but if there is some structured clusters results they could be plotted using Incanter and possible the Gorilla repl which I've just discovered.

## Running on Amazon's [Elastic MapReduce](https://aws.amazon.com/elasticmapreduce/)

Talk about using Lemur to work with EMR. Perhaps some best practices if I can remember them from the AWS summit. Worth bringing the Hadoop VM that Alex referenced in the Snowplow Google groups thread which probably allows you to run a local cluster.

## Conclusions

Sum up.
