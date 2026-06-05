package com.noom.interview.fullstack.sleep

import java.time.LocalDate


class DuplicateSleepLogException(sleepDate: LocalDate) :
    RuntimeException("A sleep log already exists for this user on $sleepDate")
