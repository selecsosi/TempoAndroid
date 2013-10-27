TempoAndroid
============

Example Android app that stores sensor measurements to TempoDB once per minute. 

The [TempoDB Java library](https://github.com/tempodb/tempodb-java) relies on a newer version of HTTPClient than what's provided by Android. 
I got around this by refactoring the TempoDB library to point to [HTTPClientAndroidLib](https://code.google.com/p/httpclientandroidlib/) instead.
