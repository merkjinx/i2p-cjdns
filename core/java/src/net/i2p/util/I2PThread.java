package net.i2p.util;

/*
 * free (adj.): unencumbered; not under the control of others
 * Written by jrandom in 2003 and released into the public domain 
 * with no warranty of any kind, either expressed or implied.  
 * It probably won't make your computer catch on fire, or eat 
 * your children, but it might.  Use at your own risk.
 *
 */


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * In case its useful later...
 * (e.g. w/ native programatic thread dumping, etc)
 *
 */
public class I2PThread extends Thread {
    private static volatile Log _log;
    private static Set _listeners = new HashSet(4);
    private String _name;
    private Exception _createdBy;

    public I2PThread() {
        super();
        if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
            _createdBy = new Exception("Created by");
    }

    public I2PThread(String name) {
        super(name);
        if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
            _createdBy = new Exception("Created by");
    }

    public I2PThread(Runnable r) {
        super(r);
        if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
            _createdBy = new Exception("Created by");
    }

    public I2PThread(Runnable r, String name) {
        super(r, name);
        if ( (_log == null) || (_log.shouldLog(Log.DEBUG)) )
            _createdBy = new Exception("Created by");
    }
    
    private void log(int level, String msg) { log(level, msg, null); }
    private void log(int level, String msg, Throwable t) {
        // we cant assume log is created
        if (_log == null) _log = new Log(I2PThread.class);
        if (_log.shouldLog(level))
            _log.log(level, msg, t);
    }

    public void run() {
        _name = Thread.currentThread().getName();
        log(Log.DEBUG, "New thread started: " + _name, _createdBy);
        try {
            super.run();
        } catch (Throwable t) {
            try {
                log(Log.CRIT, "Killing thread " + getName(), t);
            } catch (Throwable woof) {
                System.err.println("Died within the OOM itself");
                t.printStackTrace();
            }
            if (t instanceof OutOfMemoryError)
                fireOOM((OutOfMemoryError)t);
        }
        log(Log.DEBUG, "Thread finished gracefully: " + _name);
    }
    
    protected void finalize() throws Throwable {
        log(Log.DEBUG, "Thread finalized: " + _name);
        super.finalize();
    }
    
    private void fireOOM(OutOfMemoryError oom) {
        for (Iterator iter = _listeners.iterator(); iter.hasNext(); ) {
            OOMEventListener listener = (OOMEventListener)iter.next();
            listener.outOfMemory(oom);
        }
    }

    /** register a new component that wants notification of OOM events */
    public static void addOOMEventListener(OOMEventListener lsnr) {
        _listeners.add(lsnr);
    }

    /** unregister a component that wants notification of OOM events */    
    public static void removeOOMEventListener(OOMEventListener lsnr) {
        _listeners.remove(lsnr);
    }

    public interface OOMEventListener {
        public void outOfMemory(OutOfMemoryError err);
    }

    public static void main(String args[]) {
        I2PThread t = new I2PThread(new Runnable() {
            public void run() {
                throw new NullPointerException("blah");
            }
        });
        t.start();
        try {
            Thread.sleep(10000);
        } catch (Throwable tt) { // nop
        }
    }
}