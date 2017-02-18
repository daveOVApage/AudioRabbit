package cz.malyzajic.audiorabbit;

/**
 *
 * @author daop
 */
public interface IContentContainer {

    void setFiller(IMediaFinder finder);

    void fillContainer();

    void fillContainer(IMediaFinder finder);

    ContentNode getNode(String id);

    boolean hasNode(String id);

}
