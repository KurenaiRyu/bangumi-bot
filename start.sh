#!/bin/bash
java -Xms100m -Xmx200m -XX:+UseZGC -XX:+ZGenerational -XX:SoftMaxHeapSize=130m -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar build/libs/bangumi-bot.jar
