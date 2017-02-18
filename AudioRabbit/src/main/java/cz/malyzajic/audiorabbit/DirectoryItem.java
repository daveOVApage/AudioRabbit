package cz.malyzajic.audiorabbit;

import java.io.File;

/**
 *
 * @author daop
 */
public class DirectoryItem {

    private File file;
    private String label;
    private String container;
    private String id;
    private String parentId;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "DirectoryItem{" + "file=" + file + ", label=" + label + ", container=" + container + ", id=" + id + ", parentId=" + parentId + '}';
    }

}
