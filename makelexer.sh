#!/bin/sh
# Copyright © 2016 Taylor C. Richberger <taywee@gmx.com>
# This code is released under the license described in the LICENSE file

usage() {
    cat <<HERE
$0 [options]
    -h  Show this help
HERE
}

while getopts h opt; do
    case $opt in
        h)  usage
            exit
            ;;
        ?)  usage
            exit 2
            ;;
    esac
done

shift $((OPTIND - 1))

DIR="$(dirname "$0")"

java -jar "$DIR"/lib/antlr-4.5.3-complete.jar "$DIR"/src/ublu/lexer/Ublu.g4
