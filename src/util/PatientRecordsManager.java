package util;

import java.io.*;
import java.util.*;

import entities.*;

public class PatientRecordsManager {
    private final String filePath = "../Database/records";
    private HashMap<String, ArrayList<PatientRecords>> records;
    private PersonRepository personRepository;

    public PatientRecordsManager(PersonRepository p) {
        this.personRepository = p;
        records = new HashMap<String, ArrayList<PatientRecords>>();
        readRecords();
    }

    public void saveRecords() {
        File recordFile = new File(filePath);
    
        // Attempt to create the file if it doesn't exist
        if (!recordFile.exists()) {
            try {
                if (!recordFile.createNewFile()) {
                    System.err.println("Failed to create the record file.");
                    return;
                }
            } catch (IOException e) {
                System.err.println("An error occurred while creating the record file.");
                e.printStackTrace();
                return;
            }
        }
    
        // try-with-resources to ensure PrintWriter is closed properly
        try (PrintWriter writer = new PrintWriter(recordFile)) {
            for (Map.Entry<String, ArrayList<PatientRecords>> entry : records.entrySet()) {
                writer.println(entry.getKey());
                for (PatientRecords record : entry.getValue()) {
                    writer.println(record);
                }
                writer.println("---");
            }
        } catch (FileNotFoundException e) {
            System.err.println("Record file not found: " + recordFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    public void readRecords() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String patientId = line;
                ArrayList<PatientRecords> patientRecords = readPatientRecords(bufferedReader, patientId);
                records.put(patientId, patientRecords);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<PatientRecords> readPatientRecords(BufferedReader bufferedReader, String patientId) throws IOException {
        ArrayList<PatientRecords> temp = new ArrayList<>();
        String line;
        while (!(line = bufferedReader.readLine()).equals("---")) {
            if (line.startsWith("Doctor")) {
                temp.add(parseRecordEntry(line, patientId));
            } else {
                addRecordToLatestRecord(temp, line);
            }
        }
        return temp;
    }

    private PatientRecords parseRecordEntry(String line, String patientId) {
        String[] parts = line.split(",");
        String doctorId = parts[0].substring(7);
        String nurseId = parts[1].substring(6);
        Division division = personRepository.getPersonFromId(doctorId).getDivision();
        return new PatientRecords(patientId, doctorId, nurseId, division);
    }

    private void addRecordToLatestRecord(ArrayList<PatientRecords> records, String line) {
        String[] parts = line.split(":", 2);
        if (!records.isEmpty()) {
            records.get(records.size() - 1).addRecord(new PatientRecordEntry(parts[1], parts[0]));
        }
    }

    public PatientRecords getRecord(String patientId, String doctorOrNurseId) {
        ArrayList<PatientRecords> patientRecords = records.get(patientId);
        if (patientRecords == null) {
            return null;  // No records for the given patient ID
        }

        for (PatientRecords record : patientRecords) {
            if (matchesDoctorOrNurse(record, doctorOrNurseId)) {
                return record;
            }
        }
        return null;  // No matching record found
    }

    private boolean matchesDoctorOrNurse(PatientRecords record, String doctorOrNurseId) {
        return record.getDoctorId().equals(doctorOrNurseId) || record.getNurseId().equals(doctorOrNurseId);
    }


    public ArrayList<PatientRecords> getRecords(String patientId) {
        if (records.containsKey(patientId)) {
            return records.get(patientId);
        }
        return null;
    }

    public ArrayList<Patient> getPatientsForPerson(Person person) {
        ArrayList<Patient> patients = new ArrayList<>();

        for (ArrayList<PatientRecords> recordList : records.values()) {
            addPatientsForPerson(person, recordList, patients);
        }

        return patients;
    }

    private void addPatientsForPerson(Person person, ArrayList<PatientRecords> recordList, ArrayList<Patient> patients) {
        for (PatientRecords record : recordList) {
            if (record.isNurseOrDoctor(person)) {
                patients.add(getPatientById(record.getPatientId()));
            }
        }
    }

    private Patient getPatientById(String patientId) {
        return (Patient) personRepository.getPersonFromId(patientId);
    }

    public void deleteRecord(String patientId) {
        records.remove(patientId);
    }

    public boolean addRecord(String patientId, Doctor doctor, String nurseId) {
        PatientRecords newRecord = createRecord(patientId, doctor, nurseId);
    
        if (!isValidRecord(patientId, nurseId, doctor)) {
            return false;
        }
    
        if (!records.containsKey(patientId)) {
            records.put(patientId, initializeRecordList(newRecord));
            return true;
        }
    
        if (getRecord(patientId, doctor.getId()) == null) {
            records.get(patientId).add(newRecord);
            return true;
        }
    
        return false;
    }
    
    private PatientRecords createRecord(String patientId, Doctor doctor, String nurseId) {
        return new PatientRecords(patientId, doctor.getId(), nurseId, doctor.getDivision());
    }
    
    private boolean isValidRecord(String patientId, String nurseId, Doctor doctor) {
        Person patient = personRepository.getPersonFromId(patientId);
        Person nurse = personRepository.getPersonFromId(nurseId);
    
        if (!(patient instanceof Patient) || !(nurse instanceof Nurse)) {
            return false;
        }
    
        Division division = doctor.getDivision();
        return patient.getDivision().equals(division) && nurse.getDivision().equals(division);
    }
    
    private ArrayList<PatientRecords> initializeRecordList(PatientRecords newRecord) {
        ArrayList<PatientRecords> recordList = new ArrayList<>();
        recordList.add(newRecord);
        return recordList;
    }
    
}
