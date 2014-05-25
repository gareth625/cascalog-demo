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

I'll refer to _resources/data/ml-100k_ through the demo although you should use the path you downloaded and extracted the data to.

## Running on Amazon's [Elastic MapReduce](https://aws.amazon.com/elasticmapreduce/)

