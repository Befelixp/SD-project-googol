#!/bin/bash

id=$(date +%N) 
input_path="config/client.properties"

if [ $# -gt 0 ]; then
    if [[ $1 == *[^0-9]* ]]; then 
        echo "ID must be a number!"
        exit 1
    else
        id=$1
    fi
fi

if [ $# -gt 1 ]; then
    if [ -f $2 ]; then
        if [[ $2 == *.properties ]]; then
            input_path=$2
        else
            echo "Must be a properties file!"
            exit 1
        fi
    else
        echo "The file does not exist!"
        exit 1
    fi
fi

java -cp .. meta1sd.RMIClient $id ../$input_path

cd ..