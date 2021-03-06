package org.vaadin.viritin.testdomain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matti Tahvonen
 */
public class Service {

    static final Random r = new Random(0);

    static final List<Group> groups = Arrays.asList(new Group("Athletes"),
            new Group("Nerds"), new Group("Collegues"), new Group("Students"),
            new Group("Hunting club members"));

    public static Person getPerson() {
        Person p = new Person(123, "Matti", "Meikäläinen", 6);
        p.getGroups().add(groups.get(1));
        return p;
    }

    public static List<Group> getAvailableGroups() {
        return new ArrayList(groups);
    }

    public static List<Person> getListOfPersons(int total) {
        List<Person> l = new ArrayList<Person>(total);
        for (int i = 0; i < total; i++) {
            Person p = new Person();
            p.setId(i + 1);
            p.setFirstName("First" + i);
            p.setLastName("Lastname" + i);
            p.setAge(r.nextInt(100));
            l.add(p);
        }
        return l;
    }

    private static final long COUNT = 1000;
    private static List<Person> pagedBase;
    private static final Logger logger = Logger.getLogger(Service.class.getName());

    public static List<Person> findAll(long start, long maxResults) {
//        logger.log(Level.INFO, "findAll {0} {1}", new Object[]{start, maxResults});
        if (pagedBase == null) {
            pagedBase = getListOfPersons((int) COUNT);
        }
        int end = (int) (start + maxResults);
        if (end > pagedBase.size()) {
            end = pagedBase.size();
        }
        return pagedBase.subList((int) start, end);
    }

    public static long count() {
        logger.log(Level.INFO, "count");
        if (pagedBase == null) {
            pagedBase = getListOfPersons((int) COUNT);
        }
        Logger.getAnonymousLogger().entering(Service.class.getName(), "count");
        return COUNT;
    }

}
