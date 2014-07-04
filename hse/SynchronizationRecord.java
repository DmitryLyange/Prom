package hse;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: Lyange Dmitry
 * Date: 03.07.14
 * Time: 13:33
 */
public class SynchronizationRecord {

    private static int counter = 1; // counts number of objects created in order to generate new names and keys for
    // synchronizations

    private String synchronisationUuid; // id of synchronisation
    private String synchronisationName; // name of synchronisation
    private String synchronisationKey; // key of synchronisation

    private String systemNetTransitionName; // name of synchronised transition in system net
    private String eventNetTransitionName; // name of synchronised transition in event net
    private String systemNetTransitionUuid; // id of synchronised transition in system net
    private String eventNetTransitionUuid; // id of synchronised transition in event net

    /**
     * Class constructor
     *
     * @param systemNetTransitionName   name of the transition in the system net
     * @param eventNetTransitionName    name of the transition in the event net
     * @param systemNetTransitionUuid   id of the transition in the system net
     * @param eventNetTransitionUuid    id of the transition in the event net
     */
    public SynchronizationRecord(String systemNetTransitionName, String eventNetTransitionName,
                                 String systemNetTransitionUuid, String eventNetTransitionUuid) {

        synchronisationUuid = "npn" + UUID.randomUUID().toString();
        synchronisationName = "sync lam" + counter;    // TODO
        synchronisationKey = "lambda" + counter;  // TODO

        this.systemNetTransitionName = systemNetTransitionName;
        this.eventNetTransitionName = eventNetTransitionName;
        this.systemNetTransitionUuid = systemNetTransitionUuid;
        this.eventNetTransitionUuid = eventNetTransitionUuid;

        counter ++;
    }

    /**
     * Retrieves the name of system transition if parameter is <code>true</code> or the name of event transition if
     * parameter is <code>false</code>
     *
     * @param isSystemNet   parameter shows if transition required belongs to system net
     * @return name of requested transition
     */
    public String getTransitionName(boolean isSystemNet) {

        if (isSystemNet) {
            return systemNetTransitionName;
        } else {
            return eventNetTransitionName;
        }
    }

    /**
     * Retrieves the id of system transition if parameter is <code>true</code> or the id of event transition if
     * parameter is <code>false</code>
     *
     * @param isSystemNet   parameter shows if transition required belongs to system net
     * @return id of requested transition
     */
    public String getTransitionUuid(boolean isSystemNet) {

        if (isSystemNet) {
            return systemNetTransitionUuid;
        } else {
            return eventNetTransitionUuid;
        }
    }

    /**
     * Retrieves uuid of this synchronization record
     *
     * @return uuid of this synchronization
     */
    public String getSynchronisationUuid() {
        return synchronisationUuid;
    }

    /**
     * Retrieves the name of this synchronization record
     *
     * @return name of this synchronization
     */
    public String getSynchronisationName() {
        return synchronisationName;
    }

    /**
     * Retrieves the key of this synchronization record
     *
     * @return key of this synchronization
     */
    public String getSynchronisationKey() {
        return synchronisationKey;
    }
}
