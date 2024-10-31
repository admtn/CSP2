package util;

import java.util.ArrayList;
import java.util.List;

import entities.Division;
import entities.Person;

public class PatientRecords {
    private final List<PatientRecordEntry> records;
    private final String patientId;
    private final String doctorId;
    private final String nurseId;
    private final Division division;

    public PatientRecords(String patientId, String doctorId, String nurseId, Division division) {
        this.records = new ArrayList<>();
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.nurseId = nurseId;
        this.division = division;
    }

    public String getDoctorId() {
        return doctorId;
    }


    public String getNurseId() {
        return nurseId;
    }

    public String getDivisionId() {
        return division.getId();
    }


    public String getPatientId() {
        return patientId;
    }

    public void addRecord(PatientRecordEntry record) {
        records.add(record);
    }

    public boolean isNurseOrDoctor(Person person) {
        String pID = person.getId();
        return pID.equals(doctorId) || pID.equals(nurseId);
    }


    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("Doctor=").append(doctorId)
                                                .append(",Nurse=").append(nurseId);
        for (PatientRecordEntry record : records) {
            output.append("\n").append(record);
        }
        return output.toString();
    }
}
