package db.juhaku.juhakudb.test.bean;

import java.util.List;

import db.juhaku.juhakudb.annotation.Entity;
import db.juhaku.juhakudb.annotation.Id;
import db.juhaku.juhakudb.annotation.ManyToMany;
import db.juhaku.juhakudb.annotation.OneToMany;

/**
 * Created by juha on 22/12/15.
 *
 * @author juha
 */
@Entity(name = "persons")
public class Person {

    @Id
    private Integer id;

    private String username;

    @ManyToMany
    private List<ClassRoom> rooms;

    @OneToMany
    private List<Group> groups;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<ClassRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<ClassRoom> rooms) {
        this.rooms = rooms;
    }
}
