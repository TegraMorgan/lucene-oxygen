#!/bin/sh

while read line
do
    printf "\"%s\", " "$line"
done < "$1"

echo

exit 0

