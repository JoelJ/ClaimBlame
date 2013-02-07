ClaimBlame
==========

A very basic plugin for Jenkins that allows you to assign test failures to users.
The assignments are remembered until the test starts passing again.

More specifically, this plugin allows you to assign specific test failures in one build.
Those assignments persist as long as that test is failing.

So I can assign myself *failing.Test* on *build 1*,
and still see that I'm working on fixing that test on *build 5* if it's still failing.
