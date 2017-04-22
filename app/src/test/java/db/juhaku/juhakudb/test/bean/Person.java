package db.juhaku.juhakudb.test.bean;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

/**
 * Created by juha on 22/12/15.
 *
 * @author juha
 */
@Entity
public class Person {

    @Id
    private Integer id;

    private String username;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<ClassRoom> rooms;

    @OneToMany(fetch = FetchType.LAZY)
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
