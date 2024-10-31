package entities;

public class Patient extends Person {
    private String doctorID;
    public Patient(String name, Division div, String id, String doctorID) {
        super(name, id, div);
        this.doctorID = doctorID;
    }
}
