package db.juhaku.juhakudb.test.bean;

import db.juhaku.juhakudb.annotation.Entity;
import db.juhaku.juhakudb.annotation.Id;
import db.juhaku.juhakudb.annotation.OneToOne;

/**
 * Created by juha on 06/04/16.
 *
 * @author juha
 */
@Entity
public class Teacher {

    @Id
    private Integer id;

    private String name;

    @OneToOne
    private ClassRoom classRoom;
}
