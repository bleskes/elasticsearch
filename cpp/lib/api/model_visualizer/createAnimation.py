#!/usr/bin/env python

import sys,os
import subprocess
import traceback

prepend_read_file = './state_clean_'

def main(final_frame, personID):
    print personID + '...'
    file_to_write = 'resultdir/'+personID
    call_string = "printf '# name: A\n# type: cell\n# rows: 2\n# columns: {0}\n' >"\
                       " {1}".format(str(final_frame), file_to_write)
    subprocess.call(call_string, shell=True)
    subprocess.call("mkdir resultdir", shell=True)

    for i in range(1,final_frame+1):
        file_to_read = prepend_read_file+str(i)
#        print file_to_read
#        print "./model_visualiser --by '{0}' {1} >> {2}".format(personID,\
#        												file_to_read, file_to_write)
        subprocess.call("$CPP_SRC_HOME/lib/api/model_visualizer/model_visualiser --by '{0}' {1} >> {2}".format(personID,\
                                                file_to_read,file_to_write), shell=True)   

if __name__ == '__main__':
    try:
        if len(sys.argv) != 2:
            raise RuntimeError('Comparison requires 1 command line arguments - ' +\
                               str(len(sys.argv) - 1) + \
                               ' supplied. give me a personID')
        else:
            final_frame = int(sys.argv[1])
            personID = str(sys.argv[2])
            #print sys.argv[1]
            #print sys.argv[2]
            main(final_frame, personID)
    except:
        stack = traceback.format_exc()
        print stack


