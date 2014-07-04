package hse;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: Lyange Dmitry
 * Date: 30.06.14
 * Time: 16:35
 */
public class Arc {

    private String type; // takes values "ArcTP" or "ArcPT"
    private String uuid;
    private String source;
    private String target;

    /**
     * Class constructor
     *
     * @param source    source of this arc
     * @param target    target of this arc
     * @param type      type of this arc
     */
    public Arc(String source, String target, String type) {

        this.type = type;
        this.source = source;
        this.target = target;
        uuid = "npn" + UUID.randomUUID().toString();
    }

    /**
     * Retrieves uuid of this arc
     *
     * @return uuid of this arc
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Retrieves the name of the starting edge of this arc
     *
     * @return source of this arc
     */
    public String getSource() {
        return source;
    }

    /**
     * Retrieves the name of the ending edge of this arc
     *
     * @return target of this arc
     */
    public String getTarget() {
        return target;
    }

    /**
     * Retrieves the type of this arc, which depends on place/transition nature of target and source of this arc
     *
     * @return type of this arc
     */
    public String getType() {
        return type;
    }
}
