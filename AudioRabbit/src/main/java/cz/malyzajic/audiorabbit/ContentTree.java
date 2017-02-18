package cz.malyzajic.audiorabbit;

import cz.malyzajic.audiorabbit.utils.Helpers;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.seamless.util.MimeType;

/**
 *
 * @author daop
 */
public class ContentTree implements IContentContainer {

    public final static String ROOT_ID = "0";
    public final static String VIDEO_ID = "1";
    public final static String AUDIO_ID = "2";
    public final static String IMAGE_ID = "3";
    public final static String FILE_ID = "4";
    public final static String VIDEO_PREFIX = "video-item-";
    public final static String AUDIO_PREFIX = "audio-item-";
    public final static String IMAGE_PREFIX = "image-item-";
    public final static String FILE_PREFIX = "file-item-";

    private IMediaFinder mediaFinder;

    private HashMap<String, ContentNode> contentMap;

    private ContentNode rootNode;

    public ContentTree() {
        contentMap = new HashMap<>();
        rootNode = createRootNode();
    }

    protected ContentNode createRootNode() {
        // create root container
        Container root = new Container();
        root.setId(ROOT_ID);
        root.setParentID("-1");
        root.setTitle(App.fullAppName + " root directory");
        root.setCreator(App.fullManufacturer);
        root.setRestricted(true);
        root.setSearchable(true);
        root.setWriteStatus(WriteStatus.NOT_WRITABLE);
        root.setChildCount(0);
        ContentNode newRootNode = new ContentNode(ROOT_ID, root);
        contentMap.put(ROOT_ID, newRootNode);
        return newRootNode;
    }

    public ContentNode getRootNode() {
        return rootNode;
    }

    @Override
    public ContentNode getNode(String id) {
        if (contentMap.containsKey(id)) {
            return contentMap.get(id);
        }
        return null;
    }

    @Override
    public boolean hasNode(String id) {
        return contentMap.containsKey(id);
    }

    public void addNode(String ID, ContentNode Node) {
        contentMap.put(ID, Node);
    }

    public void resetRootNode() {
        contentMap = new HashMap<>();
        rootNode = createRootNode();
    }

    protected void fillContainer(List<DirectoryItem> items) {
        System.out.println("Start filling media conteiner");
        for (int i = 0; i < items.size(); i++) {
            System.out.print(".");
            DirectoryItem fileItem = items.get(i);
            AudioFile af;
            ContentNode mainNode = getNode(fileItem.getParentId());
            if (fileItem.getFile() == null || fileItem.getFile().isDirectory()) {
                StorageFolder sf = new StorageFolder();
                sf.setTitle(fileItem.getFile() != null ? fileItem.getFile().getName() : fileItem.getLabel());
                sf.setId(fileItem.getId());
                sf.setParentID(fileItem.getParentId());

                mainNode.getContainer().addContainer(sf);
                if (mainNode.getContainer().getChildCount() == null) {
                    mainNode.getContainer().setChildCount(1);
                }
                mainNode.getContainer().setChildCount(
                        mainNode.getContainer().getChildCount() + 1);
                addNode(fileItem.getId(),
                        new ContentNode(fileItem.getId(), sf));
            } else {
                try {
                    af = AudioFileIO.read(fileItem.getFile());
                } catch (CannotReadException
                        | IOException
                        | TagException
                        | ReadOnlyFileException
                        | InvalidAudioFrameException ex) {
                    af = null;
                }

                if (af == null) {
                    continue;
                }
                Tag tag = af.getTag();

                String id = fileItem.getId();
                String title = tag != null ? tag.getFirst(FieldKey.TITLE) : fileItem.getFile().getName();
                String creator = tag != null ? tag.getFirst(FieldKey.COMPOSER) : "system";
                String filePath = fileItem.getFile().getPath();
                String mimeTypeStr = Helpers.getMIMEType(filePath);
                long size = fileItem.getFile().length();
                long duration = af.getAudioHeader() != null ? af.getAudioHeader().getTrackLength() * 1000 : 0;

                String album = tag != null ? tag.getFirst(FieldKey.ALBUM) : "system";
                MimeType mimeType = new MimeType(mimeTypeStr.substring(0,
                        mimeTypeStr.indexOf('/')), mimeTypeStr.substring(mimeTypeStr
                        .indexOf('/') + 1));
                Res res = new Res(mimeType, size, "http://"
                        + App.getAddress() + "/" + id);

//                res.setDuration(duration / (1000 * 60 * 60) + ":"
//                        + (duration % (1000 * 60 * 60)) / (1000 * 60) + ":"
//                        + (duration % (1000 * 60)) / 1000);
                MusicTrack musicTrack = new MusicTrack(id,
                        fileItem.getParentId(), title, creator, null,
                        new PersonWithRole(creator, "Performer"), res);

                if (mainNode != null) {
                    mainNode.getContainer().addItem(musicTrack);
                    if (mainNode.getContainer().getChildCount() == null) {
                        mainNode.getContainer().setChildCount(1);
                    }
                    mainNode.getContainer().setChildCount(mainNode.getContainer().getChildCount() + 1);
                }
                addNode(id, new ContentNode(id, musicTrack,
                        filePath));
            }

        }
    }

    @Override
    public void setFiller(IMediaFinder finder) {
        this.mediaFinder = finder;
    }

    @Override
    public void fillContainer() {
        if (mediaFinder != null) {
            fillContainer(mediaFinder.findFiles());
        }
    }

    @Override
    public void fillContainer(IMediaFinder finder) {
        setFiller(finder);
        fillContainer();
    }

}
