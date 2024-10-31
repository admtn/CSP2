package entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Division {
    private final String id;
    private final String name;
    private final List<Patient> members;
    
    public Division(String id, String name) {
        this.id = id;
        this.name = name;
        this.members = new ArrayList<>();
    }
    
    public void addMember(Patient patient) {
        Objects.requireNonNull(patient, "Patient cannot be null");
        members.add(patient);
    }
    
    public List<Patient> getMembers() {
        return Collections.unmodifiableList(members);
    }
    
    public String getName() {
        return name;

    }
    public String getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Division division = (Division) o;
        return id.equals(division.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
