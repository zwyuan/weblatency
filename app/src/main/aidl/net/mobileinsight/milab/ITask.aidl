// ITask.aidl
package net.mobileinsight.milab;
import net.mobileinsight.milab.TaskObject;

// Declare any non-default types here with import statements

interface ITask {
    int getPid();
    TaskObject getOutput();
    void exit();
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
}
