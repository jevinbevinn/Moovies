import java.util.Objects;

public class Star {
    private String id;
    private String name;
    private String birthYear;

    public Star() {
    }

    public Star ( String id, String name, String birthYear ) {
        this.id = id;
        this.name = name;
        this.birthYear = birthYear;
    }

    // Set Methods
    public void setName( String name ) {
        this.name = name;
    }

    public void setBirthYear( String birthYear ) {
        this.birthYear = birthYear;
    }

    // Get Methods
    public String getName() {
        return name;
    }

    public String getBirthYear() {
        return Objects.requireNonNullElse(birthYear, "0");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Star Details - ");
        sb.append("Id:" + getId());
        sb.append(", ");
        sb.append("Name:" + getName());
        sb.append(", ");
        sb.append("Birth Year:" + getBirthYear());
        sb.append(".");

        return sb.toString();
    }
}
