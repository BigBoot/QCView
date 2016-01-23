package de.bigboot.qcircleview.updater;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;

public class Update implements Parcelable {
    private String versionName = "";
    private int versionCode = -1;
    private String[] changes = new String[0];
    private String downloadUrl = "";

    public String getVersionName() {
        return versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String[] getChanges() {
        return changes;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    private Update(String versionName, int versionCode, String[] changes, String downloadUrl) {
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.changes = changes;
        this.downloadUrl = downloadUrl;
    }

    public static class Builder {
        private String versionName;
        private int versionCode;
        private ArrayList<String> changes = new ArrayList<>();
        private String downloadUrl;

        public Builder versionName(String versionName) {
            this.versionName = versionName;
            return this;
        }

        public Builder versionCode(int versionCode) {
            this.versionCode = versionCode;
            return this;
        }

        public Builder changes(String[] changes) {
            this.changes.clear();
            this.changes.addAll(Arrays.asList(changes));
            return this;
        }

        public Builder addChange(String change) {
            this.changes.add(change);
            return this;
        }

        public Builder downloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Update build() {
            return new Update(versionName,
                    versionCode,
                    changes.toArray(new String[changes.size()]),
                    downloadUrl);
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.versionName);
        dest.writeInt(this.versionCode);
        dest.writeStringArray(this.changes);
        dest.writeString(this.downloadUrl);
    }

    protected Update(Parcel in) {
        this.versionName = in.readString();
        this.versionCode = in.readInt();
        this.changes = in.createStringArray();
        this.downloadUrl = in.readString();
    }

    public static final Parcelable.Creator<Update> CREATOR = new Parcelable.Creator<Update>() {
        public Update createFromParcel(Parcel source) {
            return new Update(source);
        }

        public Update[] newArray(int size) {
            return new Update[size];
        }
    };
}
