package cz.malyzajic.audiorabbit;

/**
 *
 * @author daop
 */
public class DbContentTree implements IContentContainer {

    private IMediaFinder mediaFinder;
    private Db db;

    public DbContentTree(Db db) {
        this.db = db;
    }

    @Override
    public void setFinder(IMediaFinder finder) {
        this.mediaFinder = finder;
    }

    @Override
    public void fillContainer() {
        fillContainer(mediaFinder);
    }

    @Override
    public void fillContainer(IMediaFinder finder) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ContentNode getNode(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasNode(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
