package db.juhaku.juhakudb.test.bean;

import java.util.List;

import db.juhaku.juhakudb.annotation.Column;
import db.juhaku.juhakudb.annotation.Entity;
import db.juhaku.juhakudb.annotation.Id;
import db.juhaku.juhakudb.annotation.ManyToOne;

/**
 * Created by juha on 06/04/16.
 *
 * @author juha
 */
@Entity(name = "groups")
public class Group {

    @Id
    private Integer id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    private Person person;
}
