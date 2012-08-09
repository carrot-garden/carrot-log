<!--

    Copyright (C) 2010-2012 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>

    All rights reserved. Licensed under the OSI BSD License.

    http://www.opensource.org/licenses/bsd-license.php

-->

### inspired by actual events

all of these great projects lacked some features that we needed:

[Adrian Petrescu / amazon-sns-log4j-appender]
(https://github.com/apetresc/amazon-sns-log4j-appender)

[Edson Yanaga / log4j-sns]
(https://github.com/insula/log4j-sns)

[Paul Smith / json-log4j-layout]
(https://github.com/Aconex/json-log4j-layout)


### features of current project

so we made a new one:

* comes as osgi bundle

* has configurable thread pool

* uses json event layout by default

* has configurable topic and subject

* uses event throttle based on silence period

* uses decoupled dependencies (scope provided)

* reads amazon credentials from external file

* does not try to create topics on demand (security requirement)

* uses configurable event signature for event caching  

### release repo
[maven central](http://search.maven.org/#search%7Cga%7C1%7Ccarrotgarden)

### snapshot repo
[sonatype snapshots](https://oss.sonatype.org/content/repositories/snapshots/)
