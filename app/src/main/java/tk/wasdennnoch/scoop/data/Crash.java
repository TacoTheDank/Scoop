package tk.wasdennnoch.scoop.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.afollestad.inquiry.annotations.Column;
import com.afollestad.inquiry.annotations.Table;

@SuppressWarnings("WeakerAccess")
@Table
public class Crash implements Parcelable {

    @SuppressWarnings("unused")
    public Crash() {
        // Required for Inquiry
    }

    public Crash(long time, String packageName, String description, String stackTrace) {
        this.time = time;
        this.packageName = packageName;
        this.description = description;
        this.stackTrace = stackTrace;
    }

    @Column(name = "_id", primaryKey = true, notNull = true, autoIncrement = true)
    public long id;
    @Column
    public long time;
    @Column
    public String packageName;
    @Column
    public String description;
    @Column
    public String stackTrace;

    // Adapter states
    public int count = 1;
    public boolean selected = false;

    private Crash(Parcel in) {
        id = in.readLong();
        time = in.readLong();
        packageName = in.readString();
        description = in.readString();
        stackTrace = in.readString();
        count = in.readInt();
        selected = in.readInt() == 1;
    }

    public static final Creator<Crash> CREATOR = new Creator<Crash>() {
        @Override
        public Crash createFromParcel(Parcel in) {
            return new Crash(in);
        }

        @Override
        public Crash[] newArray(int size) {
            return new Crash[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(time);
        dest.writeString(packageName);
        dest.writeString(description);
        dest.writeString(stackTrace);
        dest.writeInt(count);
        dest.writeInt(selected ? 1 : 0);
    }
}
