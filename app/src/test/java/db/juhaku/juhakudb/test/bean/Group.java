package db.juhaku.juhakudb.test.bean;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Created by juha on 06/04/16.
 *
 * @author juha
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "name_unique", columnNames = "name")})
public class Group {

    @Id
    private Integer id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Person person;
}
