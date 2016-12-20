#include <syslog.h>

int
main(void)
{
    for(int i = 0; i < 30000; ++i)
    {
        syslog(LOG_LOCAL0|LOG_NOTICE, "Prelert test message %d <END>", i);
    }

    return 1;
}
