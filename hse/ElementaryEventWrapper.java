package hse;

/**
 * Created with IntelliJ IDEA.
 * User: Lyange Dmitry
 * Date: 15.05.14
 * Time: 23:45
 */
public class ElementaryEventWrapper implements Comparable<ElementaryEventWrapper> {

    protected long date;
    protected String name;
    private int eventNetNumber;


    /**
     * Class constructor for inheritance usage in <code>SystemEventWrapper</code>
     *
     * @param date      date of event occurrence
     * @param name      name of event
     */
    protected ElementaryEventWrapper(long date, String name) {

        this.date = date;
        this.name = name;
    }


    /**
     * Class constructor for objects of <code>ElementaryEventWrapper</code>
     *
     * @param date              date of event occurrence
     * @param name              name of event
     * @param eventNetNumber    petrinet number to which this event belongs
     */
    public ElementaryEventWrapper(long date, String name, int eventNetNumber) {

        this.date = date;
        this.name = name;
        this.eventNetNumber = eventNetNumber;
    }


    /**
     * Overrides <code>compareTo</code> method to make comparison of two <code>ElementaryEventWrapper</code> objects
     * possible
     *
     * @param anotherEventWrapper   another event with which we compare this event
     * @return comparator for <code>ElementaryEventWrapper</code> objects
     */
    @Override
    public int compareTo(ElementaryEventWrapper anotherEventWrapper) {
        return (date < anotherEventWrapper.date ? -1 : (date == anotherEventWrapper.date ? 0 : 1));
    }


    /**
     * Retrieves the occurrence date of this event
     *
     * @return date of this event occurrence
     */
    public long getDate() {
        return date;
    }


    /**
     * Retrieves the name of this event
     *
     * @return name of this event
     */
    public String getName() {
        return name;
    }


    /**
     * Retrieves number of the petrinet to which this event belongs
     *
     * @return petrinet's number
     */
    public int getEventNetNumber() {
        return eventNetNumber;
    }
}