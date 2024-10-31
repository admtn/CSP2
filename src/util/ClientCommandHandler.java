package util;

import java.security.cert.X509Certificate;
import java.util.ArrayList;

import entities.Doctor;
import entities.Govt;
import entities.Nurse;
import entities.Patient;
import entities.Person;

public class ClientCommandHandler {

    public static final String LIST_PATIENT_RECORDS = "1";
    public static final String LIST_DIVISION_RECORDS = "2";
    public static final String READ_PATIENT_RECORD = "3";
    public static final String WRITE_PATIENT_RECORD = "4";
    public static final String CREATE_PATIENT_RECORD = "5";
    public static final String DELETE_PATIENT_RECORD = "6";

    private final PersonRepository personRepo;
    private final PatientRecordsManager recordsManager;
    private final Logger logger;

    public ClientCommandHandler() {
        this.personRepo = new PersonRepository();
        this.recordsManager = new PatientRecordsManager(personRepo);
        this.logger = new Logger();
    }

    public void save() {
        recordsManager.saveRecords();
    }

    public String handleClientInput(String clientInput, Person person) {
        String[] inputs = clientInput.trim().split("\\s+");
        String option = inputs.length > 0 ? inputs[0] : "";
        
        switch (option) {
            case LIST_PATIENT_RECORDS:
                return handleListPatientRecords(person);
            case LIST_DIVISION_RECORDS:
                return handleListDivisionRecords(person);
            case READ_PATIENT_RECORD:
                return handleReadPatientRecord(inputs, person);
            case WRITE_PATIENT_RECORD:
                return handleWritePatientRecord(inputs, person);
            case CREATE_PATIENT_RECORD:
                return handleCreatePatientRecord(inputs, person);
            case DELETE_PATIENT_RECORD:
                return handleDeletePatientRecord(inputs, person);
            case "quit":
                return "Logged off\n";
            default:
                return listOptions(person);
        }
    }

    private String handleListPatientRecords(Person person) {
        if (!(person instanceof Nurse || person instanceof Doctor)) {
            return listOptions(person);
        }

        StringBuilder response = new StringBuilder("Name : ID\n");
        for (Patient patient : recordsManager.getPatientsForPerson(person)) {
            response.append(patient).append("\n");
        }
        logger.log(person.getId(), person.getId(), "viewed associated patient records");
        response.append("\n").append(listOptions(person));
        return response.toString();
    }

    private String handleListDivisionRecords(Person person) {
        if (!(person instanceof Nurse || person instanceof Doctor)) {
            return listOptions(person);
        }

        StringBuilder response = new StringBuilder("Name:ID\n");
        for (Patient patient : person.getDivision().getMembers()) {
            response.append(patient).append("\n");
        }
        logger.log(person.getId(), person.getDivision().toString(), "viewed division patient records");
        response.append("\n").append(listOptions(person));
        return response.toString();
    }

    private String handleReadPatientRecord(String[] inputs, Person person) {
        if (person instanceof Patient) {
            return readOwnPatientRecord(person);
        } else if (inputs.length > 1) {
            String patientId = inputs[1];
            if (person instanceof Nurse || person instanceof Doctor) {
                return readPatientRecordForStaff(patientId, person);
            } else if (person instanceof Govt) {
                return readPatientRecordForAgency(patientId, person);
            }
        }
        return listOptions(person);
    }

    private String readOwnPatientRecord(Person person) {
        ArrayList<PatientRecords> records = recordsManager.getRecords(person.getId());
        StringBuilder response = new StringBuilder();

        if (records == null || records.isEmpty()) {
            response.append("You don't have any record\n");
        } else {
            for (PatientRecords record : records) {
                response.append(record).append("\n");
            }
        }
        logger.log(person.getId(), person.getId(), "read patient record");
        response.append("\n").append(listOptions(person));
        return response.toString();
    }

    private String readPatientRecordForStaff(String patientId, Person person) {
        ArrayList<PatientRecords> records = recordsManager.getRecords(patientId);
        StringBuilder response = new StringBuilder();

        if (records == null || records.isEmpty()) {
            response.append("Patient doesn't have any records\n");
        } else {
            response.append(recordsManager.getRecord(patientId, person.getId())).append("\n");
        }
        logger.log(person.getId(), patientId, "accessed patient records");
        response.append("\n").append(listOptions(person));
        return response.toString();
    }

    private String readPatientRecordForAgency(String patientId, Person person) {
        ArrayList<PatientRecords> records = recordsManager.getRecords(patientId);
        StringBuilder response = new StringBuilder();

        if (records == null || records.isEmpty()) {
            response.append("Patient doesn't have any records\n");
        } else {
            for (PatientRecords record : records) {
                response.append(record).append("\n");
            }
        }
        logger.log(person.getId(), patientId, "accessed patient records");
        response.append("\n").append(listOptions(person));
        return response.toString();
    }

    private String handleWritePatientRecord(String[] inputs, Person person) {
        if (inputs.length <= 1 || !(person instanceof Nurse || person instanceof Doctor)) {
            return listOptions(person);
        }

        String patientId = inputs[1];
        if (recordsManager.getRecord(patientId, person.getId()) == null) {
            return "Patient has no record associated with you\n\n" + listOptions(person);
        }
        return "Write information";
    }

    private String handleCreatePatientRecord(String[] inputs, Person person) {
        if (!(person instanceof Doctor) || inputs.length <= 2) {
            return listOptions(person);
        }

        String patientId = inputs[1];
        String nurseId = inputs[2];
        StringBuilder response = new StringBuilder();

        boolean isCreated = recordsManager.addRecord(patientId, (Doctor) person, nurseId);
        if (isCreated) {
            response.append("Record for patient was successfully created\n");
            logger.log(person.getId(), patientId, "created patient record");
        } else {
            response.append("Unable to create record for patient\n");
            logger.log(person.getId(), patientId, "tried to create patient record");
        }
        response.append("\n").append(listOptions(person));
        return response.toString();
    }

    private String handleDeletePatientRecord(String[] inputs, Person person) {
        if (inputs.length <= 1 || !(person instanceof Govt)) {
            return listOptions(person);
        }

        String patientId = inputs[1];
        recordsManager.deleteRecord(patientId);
        logger.log(person.getId(), patientId, "deleted patient record");
        return "Patient record was deleted\n\n" + listOptions(person);
    }

    public String writeInformation(String patientId, String information, Person person) {
        logger.log(person.getId(), patientId, "wrote to patient record");
        PatientRecords record = recordsManager.getRecord(patientId, person.getId());
        if (record != null) {
            record.addRecord(new PatientRecordEntry(information, Logger.getDate()));
        }
        return "Record was successfully written\n\n" + listOptions(person);
    }

    public String listOptions(Person person) {
        StringBuilder options = new StringBuilder();

        if (person instanceof Nurse || person instanceof Doctor) {
            options.append("Enter 1 : list patient records\n")
                   .append("Enter 2 : list division records\n")
                   .append("Enter 3 : and {patient's id} to read a patient record (e.g 3 5)\n")
                   .append("Enter 4 : and {patient id} to write a patient record (e.g 4 5)\n");
        }
        if (person instanceof Doctor) {
            options.append("Enter 5 : and {patient id} and {nurse id} to create a patient record (e.g 5 6 2)\n");
        }
        if (person instanceof Govt) {
            options.append("Enter 3 : and {patient id} to read a patient record (e.g: 3 5)\n")
                   .append("Enter 6 : and {patient id} to delete a patient record (e.g 6 5)\n");
        }
        if (person instanceof Patient) {
            options.append("Enter 3: to read your patient record\n");
        }

        options.append("Enter 'quit' to log off\n");
        return options.toString();
    }

    // Get person from certificate
    public Person getPerson(X509Certificate cert) {
        return personRepo.getPersonFromSerialNumber(cert.getSerialNumber());
    }
}
