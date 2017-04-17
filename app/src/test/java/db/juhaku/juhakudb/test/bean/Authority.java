package db.juhaku.juhakudb.test.bean;

import java.util.List;

import db.juhaku.juhakudb.annotation.Column;
import db.juhaku.juhakudb.annotation.Entity;
import db.juhaku.juhakudb.annotation.Id;
import db.juhaku.juhakudb.annotation.ManyToMany;

/**
 * Created by juha on 26/05/16.
 *
 * @author juha
 */
@Entity(name = "authorities")
public class Authority {

    @Id
    private Long id;

    private String value;

    private String longDescriptionOfAuthority;

    @ManyToMany
    private List<ClassRoom> classRooms;
}
