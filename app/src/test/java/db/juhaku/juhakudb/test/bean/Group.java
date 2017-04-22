package db.juhaku.juhakudb.test.bean;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Created by juha on 06/04/16.
 *
 * @author juha
 */
@Entity
public class Group {

    @Id
    private Integer id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Person person;
}
