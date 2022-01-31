#!/usr/bin/env sh
echo setting version to $1
rm -f src/main/java/ublu/Version.java
echo "package ublu;
import java.text.DateFormat;
import java.util.Date;

class Version {
    static String compileDateTime =DateFormat.getDateInstance(DateFormat.LONG).format(new Date());
    static String ubluVersion = \"$1\";
}" > src/main/java/ublu/Version.java