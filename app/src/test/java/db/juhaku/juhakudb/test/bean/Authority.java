package db.juhaku.juhakudb.test.bean;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * Created by juha on 26/05/16.
 *
 * @author juha
 */
@Entity
public class Authority {

    @Id
    private Long id;

    private String value;

    private String longDescriptionOfAuthority;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<ClassRoom> classRooms;
}
