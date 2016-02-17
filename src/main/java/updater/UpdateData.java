package updater;

import util.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents update data to be put on server for HT to check if there is new update
 */
public class UpdateData {

    private List<UpdateDownloadLink> hubturboVersionsDownloadLink; // NOPMD - not made final for gson

    public UpdateData() {
        hubturboVersionsDownloadLink = new ArrayList<>();
    }

    /**
     * Get UpdateDownloadLink for update.
     * @return Optional.empty() if there is no update, UpdateDownloadLink of update that can be downloaded otherwise
     */
    public Optional<UpdateDownloadLink> getLatestUpdateDownloadLinkForCurrentVersion() {
        if (hubturboVersionsDownloadLink.isEmpty()) {
            return Optional.empty();
        }

        // List the update link in descending order of version
        Collections.sort(hubturboVersionsDownloadLink, Collections.reverseOrder());

        // Get link of version that has same major version or just 1 major version up than current
        Optional<UpdateDownloadLink> updateLink = hubturboVersionsDownloadLink.stream()
                .filter(link -> Version.isVersionMajorSameOrJustOneGreaterFromCurrent(link.getVersion())).findFirst();

        if (updateLink.isPresent()) {
            return updateLink;
        }

        return Optional.empty();
    }
}
