package tk.wasdennnoch.scoop;

import android.support.annotation.Keep;
import android.util.Log;

import java.io.PrintWriter;

/**
 * A custom Throwable which impersonates another Throwable.
 * This is required to catch custom Throwable subclasses without crashing ourselves.
 *
 * Example: An app throws a custom exception, for example rx.exceptions.CompositeException.
 * Scoop catches it and sends the broadcast containing the serialized Exception.
 * When the CrashReceiver receives the crash and unparcels the extras it creates a new instance of that
 * object so you can retrieve it with Intent.getSerializableExtra() (or more precise with Bundle.getSerializable()).
 * The problem is that it's a custom subclass that Scoop doesn't know, so Scoop crashes too because of a
 * ClassNotFoundException.
 * To be most precise: The crash happens in Parcel.readSerializable(ClassLoader) in the resolveClass(ObjectStreamClass)
 * method of the ObjectInputStream. Yes, I traced that back.
 *
 * To avoid the own crash the custom Throwable gets "wrapped" in the MockThrowable.
 * This works because Scoop only cares about the description and the stack trace.
 */
@Keep
@SuppressWarnings("WeakerAccess")
public class MockThrowable extends Throwable {

    private String mockMessage;
    private String mockStackTrace;

    public MockThrowable(Throwable toMock) {
        super("Mocking Throwable: " + toMock.toString());
        mockMessage = toMock.toString();
        mockStackTrace = Log.getStackTraceString(toMock);
    }

    @Override
    public String toString() {
        return mockMessage;
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        s.print(mockStackTrace);
    }
}
