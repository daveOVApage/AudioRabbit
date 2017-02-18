/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.malyzajic.audiorabbit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.fourthline.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.fourthline.cling.support.contentdirectory.ContentDirectoryException;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;

/**
 *
 * @author daop
 */
public class MP3ContentDirectory extends AbstractContentDirectoryService {

    private IContentContainer container;

    private Map<String, Long> cache = new HashMap(10);

    public void setConteiner(IContentContainer container) {
        this.container = container;
    }

    @Override
    public BrowseResult browse(String objectID, BrowseFlag browseFlag,
            String filter,
            long firstResult, long maxResults,
            SortCriterion[] orderby) throws ContentDirectoryException {

        final AtomicInteger counter = new AtomicInteger(0);
        final AtomicInteger addcounter = new AtomicInteger(0);

        try {

            DIDLContent didl = new DIDLContent();

            ContentNode contentNode = container.getNode(objectID);

            if (contentNode == null) {
                return new BrowseResult("", 0, 0);
            }

            if (contentNode.isItem()) {
                didl.addItem(contentNode.getItem());

                return new BrowseResult(new DIDLParser().generate(didl), 1, 1);
            } else {
                if (browseFlag == BrowseFlag.METADATA) {
                    didl.addContainer(contentNode.getContainer());

                    return new BrowseResult(new DIDLParser().generate(didl), 1, 1);
                } else {
                    contentNode.getContainer()
                            .getContainers().forEach((contentContainer) -> {
                                didl.addContainer(contentContainer);
                            });
                    contentNode.getContainer().getItems().forEach((item) -> {
                        counter.incrementAndGet();
                        if (counter.get() >= firstResult && counter.get() < (firstResult + maxResults)) {
                            didl.addItem(item);
                            addcounter.incrementAndGet();
                        }
                    });
                    return new BrowseResult(new DIDLParser().generate(didl),
                            addcounter.get(),
                            addcounter.get());
                }

            }

        } catch (Exception ex) {
            throw new ContentDirectoryException(
                    ContentDirectoryErrorCode.CANNOT_PROCESS,
                    ex.toString()
            );
        }
    }

    @Override
    public BrowseResult search(String containerId,
            String searchCriteria, String filter,
            long firstResult, long maxResults,
            SortCriterion[] orderBy) throws ContentDirectoryException {
        // You can override this method to implement searching!
        return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
    }

}
