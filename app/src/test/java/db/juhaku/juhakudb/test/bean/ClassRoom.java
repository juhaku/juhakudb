package db.juhaku.juhakudb.test.bean;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

/**
 * Created by juha on 22/12/15.
 *
 * @author juha
 */
@Entity
public class ClassRoom {

    @Id
    private Integer id;

    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<Person> persons;

    @OneToOne(mappedBy = "classRoom", fetch = FetchType.LAZY)
    private Teacher teacher;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }
}
