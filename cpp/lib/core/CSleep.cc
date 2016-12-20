/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
#include <core/CSleep.h>

#include <core/CLogger.h>

#include <time.h>


namespace prelert
{
namespace core
{


// Default processing delay is 100 milliseconds
const uint32_t CSleep::DEFAULT_PROCESSING_DELAY(100);


void CSleep::sleep(uint32_t milliseconds)
{
    if (milliseconds > 0)
    {
        struct timespec delay = { milliseconds / 1000, (milliseconds % 1000) * 1000000 };

        if (::nanosleep(&delay, 0) < 0)
        {
            LOG_WARN("nanosleep interrupted");
        }
    }
}

void CSleep::delayProcessing(void)
{
    // 0.1 seconds is a good length of time to delay processing.
    // Longer than this runs the risk of unwanted MySQL disconnections.
    CSleep::sleep(DEFAULT_PROCESSING_DELAY);
}


}
}

