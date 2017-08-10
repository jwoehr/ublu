#!/usr/bin/perl
# Validate a userid / password combo
# Example from Ublu Midrange and Mainframe Life Cycle Extension language
# https://github.com/jwoehr/ublu
# Copyright (C) 2017 Jack J. Woehr http://www.softwoehr.com
# See the Ublu license (BSD-2 open source)
use strict;
use warnings;

if (@ARGV != 2 ) {
	print "Usage:\t$0 username password\n";
	print "\tMust be run superuser.\n";
	print "\tReturn: 0 ==   valid u/p combo\n";
	print "\t        1 == invalid u/p combo\n";
	print "\t        2 ==    argument error\n";
	exit 2;
}

my $username = $ARGV[0];
my $passwd = $ARGV[1];
my $theuid;
my $result;

while(my($name,$passwd,$uid,$gid,$quota,$comment,$gcos,$dir,$shell,$expire) = getpwent()){
	if ($name eq $username) {
		$theuid = $uid;
       	last;
    }
}
endpwent();

if (!length $theuid) { # No such user
	$result = 1;
} else { # User exists, validate
	my $pswd = (getpwuid($theuid))[1];
	if (crypt($passwd, $pswd) eq $pswd) {
		$result = 0;
	} else {
		$result = 1;
	}
}
	
exit $result;
