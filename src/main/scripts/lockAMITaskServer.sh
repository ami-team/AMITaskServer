#!/bin/bash

PORT=1357

#############################################################################

curl http://localhost:$PORT/?Command=LockScheduler &> /dev/null

#############################################################################

exit 0

#############################################################################
