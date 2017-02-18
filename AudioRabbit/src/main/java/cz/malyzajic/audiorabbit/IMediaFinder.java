package cz.malyzajic.audiorabbit;

import java.util.List;

/**
 *
 * @author daop
 */
public interface IMediaFinder {

    List<DirectoryItem> findFiles();

    List<DirectoryItem> findFiles(RabitConfiguration rc);

    void setConfiguration(RabitConfiguration rc);

}
