package hse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Lyange Dmitry
 * Date: 15.05.14
 * Time: 23:45
 */
public class SystemEventWrapper extends ElementaryEventWrapper {

    private List<ElementaryEventWrapper> candidatesForSynchronisation;

    /**
     * Class constructor
     *
     * @param date      date of event occurrence
     * @param name      name of event
     */
    public SystemEventWrapper(long date, String name) {

        super(date, name);
        candidatesForSynchronisation = new ArrayList<>();
    }

    /**
     * Retrieves list of possible events to be synchronized with this system event
     *
     * @return events which can be synchronized with this system event
     */
    public List<ElementaryEventWrapper> getCandidatesForSynchronisation() {
        return candidatesForSynchronisation;
    }
}