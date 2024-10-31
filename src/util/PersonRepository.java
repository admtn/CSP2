package util;

import java.math.BigInteger;
import java.util.*;

import entities.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class PersonRepository {
    private static final String DEFAULT_FILE_PATH = "../Database/ClientInfo";
    
    private final String filePath;
    private final Map<BigInteger, Person> persons;
    private final List<Division> divisions;

    public PersonRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public PersonRepository(String filePath) {
        this.filePath = filePath;
        this.persons = new HashMap<>();
        this.divisions = new ArrayList<>();
        readFile();
    }

    public Person getPersonFromSerialNumber(BigInteger serialNumber) {
        return persons.get(serialNumber);
    }

    public Person getPersonFromId(String id) {
        for (Person person : persons.values()) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return null;
    }

    public Set<Map.Entry<BigInteger, Person>> getPersons() {
        return persons.entrySet();
    }

    public Division getDivisionFromId(String id) {
        for (Division division : divisions) {
            if (division.getId().equals(id)) {
                return division;
            }
        }
        return null;
    }

    private void readFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            readDivisions(reader);
            readPersons(reader);
        } catch (IOException e) {
            System.err.println("Error reading the person information file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void readDivisions(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !line.trim().equals("---")) {
            String[] divisionInfo = line.split(":");
            if (divisionInfo.length < 2) {
                System.err.println("Invalid division line format: " + line);
                continue;
            }
            String id = divisionInfo[0].trim();
            String name = divisionInfo[1].trim();
            divisions.add(new Division(id, name));
        }
    }

    private void readPersons(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] personInfo = line.split(":");
            if (personInfo.length < 5) {
                System.err.println("Invalid person line format: " + line);
                continue;
            }

            try {
                BigInteger serialNumber = new BigInteger(personInfo[0].trim());
                String type = personInfo[1].trim();
                String divisionId = personInfo[2].trim();
                String id = personInfo[3].trim();
                String name = personInfo[4].trim();
                String additionalInfo = personInfo.length > 5 ? personInfo[5].trim() : null;

                Division division = getDivisionFromId(divisionId);
                if (division == null && !type.equalsIgnoreCase("GovernmentAgency")) {
                    System.err.println("Division not found for ID: " + divisionId + " in line: " + line);
                    continue;
                }

                Person person = createPerson(type, name, division, id, additionalInfo);
                if (person != null) {
                    persons.put(serialNumber, person);
                    if (person instanceof Patient && division != null) {
                        division.addMember((Patient) person);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid serial number format in line: " + line);
            }
        }
    }

    private Person createPerson(String type, String name, Division division, String id, String additionalInfo) {
        switch (type) {
            case "Nurse":
                return new Nurse(name, division, id);
            case "Doctor":
                return new Doctor(name, division, id);
            case "GovernmentAgency":
                return new Govt(name, id);
            case "Patient":
                if (additionalInfo == null) {
                    System.err.println("Missing additional info for Patient in line.");
                    return null;
                }
                return new Patient(name, division, id, additionalInfo);
            default:
                System.err.println("Unknown person type: " + type);
                return null;
        }
    }
}
