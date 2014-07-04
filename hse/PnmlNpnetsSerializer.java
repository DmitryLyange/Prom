package hse;

import org.deckfour.spex.SXDocument;
import org.deckfour.spex.SXTag;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: Lyange Dmitry
 * Date: 30.06.14
 * Time: 11:01
 */
public class PnmlNpnetsSerializer {

    private List<SynchronizationRecord> synchronizationRecords; // all pairs of synchronized transitions
    private List<Arc> arcs; // all arcs in petrinet
    List<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges; // list of incoming arcs
    List<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges; // list of outgoing arcs

    /**
     * Class constructor
     *
     * @param synchronizationRecords    all pairs of synchronized transitions,
     *                                  saved in <code>SynchronizationRecord</code> type
     *
     */
    public PnmlNpnetsSerializer(List<SynchronizationRecord> synchronizationRecords) {

        this.synchronizationRecords = synchronizationRecords;
    }

    /**
     * TODO
     *
     * @param petrinets     system and elementary petrinets
     * @param out           output stream
     *
     */
    public void serialize(List<Petrinet> petrinets, OutputStream out) throws IOException {

        SXDocument doc = new SXDocument(out);
        SXTag npnetsTag = doc.addNode("npnets:NPnetMarked");
        npnetsTag.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        npnetsTag.addAttribute("xmlns:hlpn", "mathtech.ru/npntool/hlpn");
        npnetsTag.addAttribute("xmlns:npndiagrams", "http:/mathtech.ru/npntool/npndiagrams");
        npnetsTag.addAttribute("xmlns:npnets", "mathtech.ru/npntool/npnets");
        String res = "npn" + UUID.randomUUID().toString();
        npnetsTag.addAttribute("id", res);

        SXTag globalNetTag = npnetsTag.addChildNode("net");
        res = "npn" + UUID.randomUUID().toString();
        globalNetTag.addAttribute("uuid", res);
        globalNetTag.addAttribute("name", "Untitled NPN");              //TODO change net name

        SXTag netSystemTag = globalNetTag.addChildNode("netSystem");
        res = "npn" + UUID.randomUUID().toString();
        netSystemTag.addAttribute("uuid", res);
        netSystemTag.addAttribute("name", "Untitled SN");         //TODO change system net name

        Petrinet systemNet = petrinets.get(0);

        addNet(netSystemTag, systemNet, true);

        SXTag typeElementNetTag = globalNetTag.addChildNode("typeElementNet");
        res = "npn" + UUID.randomUUID().toString();
        typeElementNetTag.addAttribute("uuid", res);

        Petrinet elementaryNet;
        for (int i = 1; i < petrinets.size(); i ++ ) {
            elementaryNet = petrinets.get(i);

            SXTag netTag = typeElementNetTag.addChildNode("net");
            res = "npn" + UUID.randomUUID().toString();
            netTag.addAttribute("uuid", res);

            addNet(netTag, elementaryNet, false);


        }

        for (SynchronizationRecord synchronizationRecord: synchronizationRecords) {
            SXTag synchronizationsTag = globalNetTag.addChildNode("synchronizations");
            synchronizationsTag.addAttribute("uuid", synchronizationRecord.getSynchronisationUuid());
            synchronizationsTag.addAttribute("name", synchronizationRecord.getSynchronisationName());
            synchronizationsTag.addAttribute("key", synchronizationRecord.getSynchronisationKey());
            synchronizationsTag.addAttribute("involved", "#" + synchronizationRecord.getTransitionUuid(true) +
                    " #" + synchronizationRecord.getTransitionUuid(false));
        }

        SXTag diagramNetSystemTag = npnetsTag.addChildNode("diagramNetSystem");

        doc.close();
    }


    /**
     * Adds all needed information (places, transitions, arcs, synchronisations) about given petrinet
     * 
     * @param parentTag     tag to which we add current section
     * @param currentNet    net which characteristics we want to describe
     * @param isSystemNet   boolean value which shows if current petrinet is system or event type
     *
     */
    private void addNet(SXTag parentTag, Petrinet currentNet, boolean isSystemNet) throws IOException {

        arcs = new ArrayList<>();

        addPlaces(parentTag, currentNet);

        addTransitions(parentTag, currentNet, isSystemNet);

        addArcs(parentTag);
    }


    /**
     * Adds new tag for all places in given petrinet
     *
     * @param parentTag     tag to which we add current section
     * @param currentNet    net which Places we want to describe
     *
     */
    private void addPlaces(SXTag parentTag, Petrinet currentNet ) throws IOException {

        String source;
        String target;
        boolean isNew;

        for (Place place: currentNet.getPlaces()) {
            SXTag nodesTag = parentTag.addChildNode("nodes");
            nodesTag.addAttribute("xsi:type", "hlpn:Place");
            nodesTag.addAttribute("uuid", "npn" + place.getId().toString().substring(5));
            nodesTag.addAttribute("name", place.toString());

            inEdges = new ArrayList<>(currentNet.getInEdges(place));
            outEdges = new ArrayList<>(currentNet.getOutEdges(place));

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> aInEdge : inEdges) {
                addArcsForPlace(nodesTag, aInEdge, "ArcTP");
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> aOutEdge : outEdges) {
                addArcsForPlace(nodesTag, aOutEdge, "ArcPT");
            }
        }
    }


    /**
     * Adds new tag for all transitions in given petrinet
     *
     * @param parentTag     tag to which we add current section
     * @param currentNet    net which Transitions we want to describe
     * @param isSystemNet   boolean value which shows if current petrinet is system or event
     *
     */
    private void addTransitions(SXTag parentTag, Petrinet currentNet, boolean isSystemNet ) throws IOException {

        boolean isSynced;
        for (Transition transition: currentNet.getTransitions()) {

            isSynced = false;
            String synchronisationUuid = null;

            for (SynchronizationRecord synchronizationRecord: synchronizationRecords) {
                if (synchronizationRecord.getTransitionName(isSystemNet).equals(transition.toString())) {
                    isSynced = true;
                    synchronisationUuid = synchronizationRecord.getSynchronisationUuid();
                }
            }

            SXTag nodesTag = parentTag.addChildNode("nodes");

            if (isSynced) {
                nodesTag.addAttribute("xsi:type", "npnets:TransitionSynchronized");
            } else {
                nodesTag.addAttribute("xsi:type", "hlpn:Transition");
            }

            nodesTag.addAttribute("uuid", "npn" + transition.getId().toString().substring(5));
            nodesTag.addAttribute("name", transition.toString());

            inEdges = new ArrayList<>(currentNet.getInEdges(transition));
            outEdges = new ArrayList<>(currentNet.getOutEdges(transition));

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> aInEdge : inEdges) {
                addArcsForTransition(nodesTag, aInEdge, "ArcPT");
            }

            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> aOutEdge : outEdges) {
                addArcsForTransition(nodesTag, aOutEdge, "ArcTP");
            }

            if (isSynced) {
                nodesTag.addAttribute("synchronization", "#" + synchronisationUuid);   // TODO Будет ошибка, если null?
            }
        }
    }

    /**
     * Adds new arcs to general list of arcs in the petrinet;
     * adds new attribute for each in/out arc of given place
     *
     * @param parentTag     tag to which we add current section
     * @param edge          place which arcs we want to add
     * @param arcType       type of arc (from place to transition or otherwise)                     
     *                      
     */
    private void addArcsForPlace(SXTag parentTag, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge,
                                    String arcType) throws IOException{

        String source;
        String target;
        boolean isArcNew;

        source = "npn" + edge.getSource().getId().toString().substring(5);
        target = "npn" + edge.getTarget().getId().toString().substring(5);
        isArcNew = true;

        for (Arc arc: arcs) {
            if ((arc.getSource().equals(source)) && (arc.getTarget().equals(target))) {
                isArcNew = false;

                if (arcType.equals("ArcTP")) {
                    parentTag.addAttribute("inArcs", "#" + arc.getUuid());
                } else {
                    parentTag.addAttribute("outArcs", "#" + arc.getUuid());
                }

                break;
            }
        }

        if (isArcNew) {
            arcs.add(new Arc(source, target, arcType));
            if (arcType.equals("ArcTP")) {
                parentTag.addAttribute("inArcs", "#" + arcs.get(arcs.size() - 1).getUuid());
            } else {
                parentTag.addAttribute("outArcs", "#" + arcs.get(arcs.size() - 1).getUuid());
            }
        }
    }

    /**
     * Adds new attribute for each in/out arc of given transition
     *
     * @param parentTag     tag to which we add current section
     * @param edge          transition which arcs we want to add
     * @param arcType       type of arc (from place to transition or otherwise)
     *
     */
    private void addArcsForTransition(SXTag parentTag, PetrinetEdge<? extends PetrinetNode,
            ? extends PetrinetNode> edge, String arcType) throws IOException{

        String source;
        String target;

        source = "npn" + edge.getSource().getId().toString().substring(5);
        target = "npn" + edge.getTarget().getId().toString().substring(5);

        for (Arc arc: arcs) {
            if ((arc.getSource().equals(source)) && (arc.getTarget().equals(target))) {
                if (arcType.equals("ArcTP")) {
                    parentTag.addAttribute("outArcs", "#" + arc.getUuid());
                } else {
                    parentTag.addAttribute("inArcs", "#" + arc.getUuid());
                }

                break;
            }
        }
    }

    /**
     * Adds new tag for each arc in arcs
     *
     * @param parentTag     tag to which we add current section
     *
     */
    private void addArcs(SXTag parentTag) throws IOException {

        for (Arc arc: arcs) {
            SXTag arcsTag = parentTag.addChildNode("arcs");
            arcsTag.addAttribute("xsi:type", "hlpn:" + arc.getType());
            arcsTag.addAttribute("uuid", arc.getUuid());
            arcsTag.addAttribute("source", "#" + arc.getSource());
            arcsTag.addAttribute("target", "#" + arc.getTarget());
        }
    }
}
