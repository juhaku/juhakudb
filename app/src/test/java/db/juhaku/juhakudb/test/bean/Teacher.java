package db.juhaku.juhakudb.test.bean;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

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

    @OneToOne(fetch = FetchType.LAZY)
    private ClassRoom classRoom;
}
