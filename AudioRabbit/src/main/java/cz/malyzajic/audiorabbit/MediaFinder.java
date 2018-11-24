package cz.malyzajic.audiorabbit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author daop
 */
public class MediaFinder implements IMediaFinder {

    RabitConfiguration configuration;
    Integer counter = 0;

    @Override
    public List<DirectoryItem> findFiles(RabitConfiguration rc) {
        this.configuration = rc;
        return findFiles();
    }

    @Override
    public List<DirectoryItem> findFiles() {
        List<DirectoryItem> result = new LinkedList<>();
        if (configuration.mediaDirectories != null && configuration.mediaDirectories.length > 0) {
            for (String path : configuration.mediaDirectories) {
                String key = path.replaceAll("/", "-");
                key = key.replaceFirst("-", "");
                DirectoryItem item = new DirectoryItem();
                item.setId(path + (counter++));
                item.setLabel(path);
                item.setParentId("0");
                result.add(item);
                listFilesAndFilesSubDirectories(key, path, item.getId(), result);
            }
        }

        return result;
    }

    public void listFilesAndFilesSubDirectories(String root, String directoryName, String parentId, List<DirectoryItem> list) {
        DirectoryItem item;
        File directory = new File(directoryName);

        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {

                item = new DirectoryItem();
                item.setFile(file);
                item.setId(root + (counter++) + getSuffix(file));
                item.setParentId(parentId);
                item.setContainer(root);
                list.add(item);
                if (file.isDirectory()) {
                    listFilesAndFilesSubDirectories(root, file.getAbsolutePath(), item.getId(), list);
                }
            }
        } else {
            Logger.getLogger(App.class.getName()).log(Level.INFO, "{0} is not directory", directoryName);
        }

    }

    @Override
    public void setConfiguration(RabitConfiguration rc) {
        this.configuration = rc;
    }

    private String getSuffix(File file) {
        String result = null;
        if (file != null && file.isFile()) {
            String separator = System.getProperty("file.separator");

            int indexOfLastSeparator = file.getAbsolutePath().lastIndexOf(separator);
            String filename = file.getAbsolutePath().substring(indexOfLastSeparator + 1);

            int extensionIndex = filename.lastIndexOf(".");
            result = filename.substring(extensionIndex + 1);
        }
        return result == null ? "" : "." + result;
    }

}
