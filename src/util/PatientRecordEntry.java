package util;

public class PatientRecordEntry {
    private String entry;
    private String date;

    public PatientRecordEntry(String entry, String date) {
        this.date = date;
        this.entry = entry;
    }

    public String getDate() { return date; }
    public String getEntry(){ return entry; }
    public String toString(){ return  date + ":" + entry; }
}
