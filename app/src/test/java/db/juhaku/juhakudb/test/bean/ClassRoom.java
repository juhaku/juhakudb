package db.juhaku.juhakudb.test.bean;

import java.util.List;

import db.juhaku.juhakudb.annotation.Column;
import db.juhaku.juhakudb.annotation.Entity;
import db.juhaku.juhakudb.annotation.Id;
import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.annotation.OneToOne;

/**
 * Created by juha on 22/12/15.
 *
 * @author juha
 */
@Entity
public class ClassRoom {

    @Id
    private Integer id;

    @Column(name = "name")
    private String name;

    @ManyToMany
    private List<Person> persons;

    @OneToOne(mappedBy = "classRoom")
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
