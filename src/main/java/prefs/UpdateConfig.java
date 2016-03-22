package prefs;

import util.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains configuration information for updating
 */
public class UpdateConfig {

    private boolean isLastAppUpdateDownloadSuccessful;
    private List<Version> versionsPreviouslyDownloaded; // NOPMD - not made final for gson

    public UpdateConfig() {
        versionsPreviouslyDownloaded = new ArrayList<>();
    }

    /**
     * Sets last app update download status
     * @param isLastAppUpdateDownloadSuccessful true if update download is successful, false otherwise
     */
    public void setLastAppUpdateDownloadSuccessful(boolean isLastAppUpdateDownloadSuccessful) {
        this.isLastAppUpdateDownloadSuccessful = isLastAppUpdateDownloadSuccessful;
    }

    public boolean getLastAppUpdateDownloadSuccessful() {
        return this.isLastAppUpdateDownloadSuccessful;
    }

    /**
     * Adds a version to downloaded versions
     */
    public void addToVersionPreviouslyDownloaded(Version downloadedVersion) {
        versionsPreviouslyDownloaded.add(downloadedVersion);
    }

    /**
     * Checks if a version has been downloaded previously
     */
    public boolean wasPreviouslyDownloaded(Version version) {
        return versionsPreviouslyDownloaded.contains(version);
    }
}
