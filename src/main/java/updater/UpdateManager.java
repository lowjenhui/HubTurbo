package updater;

import javafx.application.Platform;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ui.UI;
import ui.UpdateProgressWindow;
import util.DialogMessage;
import prefs.Preferences;
import prefs.UpdateConfig;
import util.JsonFileConverter;
import util.Version;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The class that will handle updating of HubTurbo application
 */
public class UpdateManager {
    private static final Logger logger = LogManager.getLogger(UpdateManager.class.getName());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    // Error messages
    private static final String ERROR_INIT_UPDATE = "Failed to initialize update";
    private static final String ERROR_DOWNLOAD_UPDATE_DATA = "Failed to download update data";
    private static final String ERROR_DOWNLOAD_UPDATE_APP = "Failed to download updated application";

    // Directories and file names
    private static final String UPDATE_DIRECTORY = "updates";
    // TODO change to release branch on merging with master
    private static final String UPDATE_SERVER_DATA_NAME =
            "https://raw.githubusercontent.com/HubTurbo/HubTurbo/1271-updater-data/HubTurboUpdate.json";
    private static final String UPDATE_LOCAL_DATA_NAME = UPDATE_DIRECTORY + File.separator + "HubTurbo.json";
    private static final String UPDATE_APP_NAME = "HubTurbo.jar";
    private static final String UPDATE_APP_PATH = UPDATE_DIRECTORY + File.separator + UPDATE_APP_NAME;
    public static final String UPDATE_CONFIG_FILENAME = "updateConfig.json";
    private static final String UPDATE_JAR_UPDATER_APP_PATH = UPDATE_DIRECTORY + File.separator + "jarUpdater.jar";

    // Constants
    private static final int MAX_HT_BACKUP_JAR_KEPT = 3;
    private static final String HT_BACKUP_FILENAME_PATTERN_STRING =
            "HubTurbo_(" + Version.VERSION_PATTERN_STRING + ")\\.(jar|JAR)$";

    // Class member variables
    private UpdateConfig updateConfig;
    private final UpdateProgressWindow updateProgressWindow;
    private final UI ui;
    private boolean applyUpdateImmediately;

    public UpdateManager(UI ui, UpdateProgressWindow updateProgressWindow) {
        this.ui = ui;
        this.updateProgressWindow = updateProgressWindow;
        this.applyUpdateImmediately = false;
        loadUpdateConfig();
    }

    /**
     * Driver method to trigger UpdateManager to run. Update will be run on another thread.
     *
     * - Run is not automatic upon instancing the class in case there would like to be conditions on when to run update,
     *   e.g. only if user is logged in
     */
    public void run() {
        pool.execute(() -> runUpdate());
    }

    /**
     * Runs update sequence.
     */
    private void runUpdate() {
        // Fail if folder cannot be created
        if (!initUpdate()) {
            logger.error(ERROR_INIT_UPDATE);
            return;
        }

        cleanupHTBackupJar();

        if (!downloadUpdateData()) {
            logger.error(ERROR_DOWNLOAD_UPDATE_DATA);
            return;
        }

        // Checks if there is a new update since last update
        Optional<UpdateDownloadLink> updateDownloadLink = getLatestUpdateDownloadLinkForCurrentVersion();

        if (!updateDownloadLink.isPresent() ||
            !checkIfNewVersionAvailableToDownload(updateDownloadLink.get().getVersion())) {
            return;
        }

        if (!downloadUpdateForApplication(updateDownloadLink.get().getApplicationFileLocation())) {
            logger.error(ERROR_DOWNLOAD_UPDATE_APP);
            return;
        }

        markAppUpdateDownloadSuccess(updateDownloadLink.get().getVersion());

        // Prompt user for restarting application to apply update
        promptUserToApplyUpdateImmediately();

    }

    /**
     * Initializes system for updates
     * - Creates directory(ies) for updates
     * - Extract jarUpdater
     */
    private boolean initUpdate() {
        logger.info("Initiating updater");
        File updateDir = new File(UPDATE_DIRECTORY);

        if (!updateDir.exists() && !updateDir.mkdirs()) {
            logger.error("Failed to create update directories");
            return false;
        }

        updateConfig.setLastUpdateDownloadStatus(false);
        saveUpdateConfig();

        return extractJarUpdater();
    }

    private boolean extractJarUpdater() {
        logger.info("Extracting jarUpdater");
        File jarUpdaterFile = new File(UPDATE_JAR_UPDATER_APP_PATH);

        try {
            jarUpdaterFile.createNewFile();
        } catch (IOException e) {
            logger.error("Can't create empty file for jarUpdater");
            return false;
        }

        try (InputStream in = UpdateManager.class.getClassLoader().getResourceAsStream("updater/jarUpdater");
             OutputStream out = new FileOutputStream(jarUpdaterFile)) {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            logger.error("Can't extract jarUpdater", e);
            return false;
        }

        return true;
    }

    /**
     * Keeps the number of HT Jar in the folder used as backup to specified amount.
     */
    private void cleanupHTBackupJar() {
        logger.info("Cleaning up backup JAR");

        File currDirectory = new File(".");

        File[] filesInCurrentDirectory = currDirectory.listFiles();

        if (filesInCurrentDirectory == null) {
            // current directory always exists
            assert false;
            return;
        }
        assert filesInCurrentDirectory != null;

        List<File> listOfFilesInCurrDirectory = Arrays.asList(filesInCurrentDirectory);

        List<File> allHtBackupFiles = listOfFilesInCurrDirectory.stream()
                .filter(f -> !f.getName().equals(String.format("HubTurbo_%s.jar", Version.getCurrentVersion())) &&
                             f.getName().matches(HT_BACKUP_FILENAME_PATTERN_STRING))
                .sorted(getHtBackupFileComparatorByVersion())
                .collect(Collectors.toList());

        if (allHtBackupFiles.isEmpty()) {
            return;
        }

        for (int i = 0; i < (allHtBackupFiles.size() - MAX_HT_BACKUP_JAR_KEPT); i++) {
            logger.info("Deleting " + allHtBackupFiles.get(i).getName());
            if (!allHtBackupFiles.get(i).delete()) {
                logger.warn("Failed to delete old HT backup file " + allHtBackupFiles.get(i).getName());
            }
        }
    }

    private Comparator<File> getHtBackupFileComparatorByVersion() {
        return (a, b) -> getVersionOfHtBackupFileFromFilename(a.getName())
                .compareTo(getVersionOfHtBackupFileFromFilename(b.getName()));
    }

    /**
     * Gets version of HubTurbo from Jar backup file.
     * Expects filename in format "HubTurbo_V[major].[minor].[patch].jar".
     * @param filename filename of HT backup JAR, in format "HubTurbo_V[major].[minor].[patch].jar"
     * @return version of HT of backup JAR
     */
    private Version getVersionOfHtBackupFileFromFilename(String filename) {
        Pattern htJarFileBackupPattern = Pattern.compile(HT_BACKUP_FILENAME_PATTERN_STRING);
        Matcher htJarFileBackupMatcher = htJarFileBackupPattern.matcher(filename);
        if (!htJarFileBackupMatcher.find()) {
            assert false;
        }

        return Version.fromString(htJarFileBackupMatcher.group(1));
    }

    /**
     * Downloads update data to check if update is present.
     *
     * @return true if download successful, false otherwise
     */
    private boolean downloadUpdateData() {
        logger.info("Downloading update data");
        try {
            FileDownloader fileDownloader = new FileDownloader(
                    new URI(UPDATE_SERVER_DATA_NAME),
                    new File(UPDATE_LOCAL_DATA_NAME),
                    a -> {});
            return fileDownloader.download();
        } catch (URISyntaxException e) {
            logger.error(ERROR_DOWNLOAD_UPDATE_DATA, e);
            return false;
        }
    }

    /**
     * Downloads application update based on update data.
     *
     * @return true if download successful, false otherwise
     */
    private boolean downloadUpdateForApplication(URL downloadURL) {
        logger.info("Downloading update for application");
        URI downloadUri;

        try {
            downloadUri = downloadURL.toURI();
        } catch (URISyntaxException e) {
            logger.error("Download URI is not correct", e);
            return false;
        }

        LabeledDownloadProgressBar downloadProgressBar = updateProgressWindow.createNewDownloadProgressBar(
                downloadUri, "Downloading HubTurbo Application...");

        FileDownloader fileDownloader = new FileDownloader(
                downloadUri,
                new File(UPDATE_APP_PATH),
                downloadProgressBar::setProgress);
        boolean result = fileDownloader.download();

        updateProgressWindow.removeDownloadProgressBar(downloadUri);

        return result;
    }

    /**
     * Checks if a given version is a new version that can be downloaded.
     * If that version was previously downloaded (even if newer than current), we will not download it again.
     *
     * Scenario: on V0.0.0, V1.0.0 was downloaded. However, user is still using V0.0.0 and there is no newer update
     *           than V1.0.0. HT won't download V1.0.0 again because the fact that user is still in V0.0.0 means he
     *           does not want to use V0.0.0 (either V1.0.0 is broken or due to other reasons).
     *
     * @param version version to be checked if it is an update
     * @return true if the given version can be downloaded, false otherwise
     */
    private boolean checkIfNewVersionAvailableToDownload(Version version) {
        return Version.getCurrentVersion().compareTo(version) < 0 &&
                !updateConfig.checkIfVersionWasPreviouslyDownloaded(version);
    }

    /**
     * Get latest HT version available to download
     * @return download link of a HT version
     */
    private Optional<UpdateDownloadLink> getLatestUpdateDownloadLinkForCurrentVersion() {
        File updateDataFile = new File(UPDATE_LOCAL_DATA_NAME);
        JsonFileConverter jsonUpdateDataConverter = new JsonFileConverter(updateDataFile);
        UpdateData updateData = jsonUpdateDataConverter.loadFromFile(UpdateData.class).orElse(new UpdateData());

        return updateData.getLatestUpdateDownloadLinkForCurrentVersion();
    }

    private void markAppUpdateDownloadSuccess(Version versionDownloaded) {
        updateConfig.setLastUpdateDownloadStatus(true);
        updateConfig.addToVersionPreviouslyDownloaded(versionDownloaded);
        saveUpdateConfig();
    }

    private void loadUpdateConfig() {
        File updateConfigFile = new File(Preferences.DIRECTORY + File.separator + UPDATE_CONFIG_FILENAME);
        JsonFileConverter jsonConverter = new JsonFileConverter(updateConfigFile);
        this.updateConfig = jsonConverter.loadFromFile(UpdateConfig.class).orElse(new UpdateConfig());
    }

    private void saveUpdateConfig() {
        File updateConfigFile = new File(Preferences.DIRECTORY + File.separator + UPDATE_CONFIG_FILENAME);
        JsonFileConverter jsonConverter = new JsonFileConverter(updateConfigFile);
        try {
            jsonConverter.saveToFile(updateConfig);
        } catch (IOException e) {
            logger.warn("Failed to save Update Config", e);
        }
    }

    /**
     * Runs updating clean up on quitting HubTurbo
     */
    public void onAppQuit() {
        if (!applyUpdateImmediately && updateConfig.getLastUpdateDownloadStatus()) {
            updateConfig.setLastUpdateDownloadStatus(false);
            saveUpdateConfig();

            runJarUpdaterWithoutExecute();
        }
    }

    public void showUpdateProgressWindow() {
        updateProgressWindow.showWindow();
    }

    public void hideUpdateProgressWindow() {
        updateProgressWindow.hideWindow();
    }

    private boolean runJarUpdaterWithExecute() {
        return runJarUpdater(true);
    }

    private boolean runJarUpdaterWithoutExecute() {
        return runJarUpdater(false);
    }

    private boolean runJarUpdater(boolean shouldExecuteJar) {
        String restarterAppPath = UPDATE_JAR_UPDATER_APP_PATH;
        String cmdArg = String.format("--source=%s --target=%s --execute-jar=%s --backup-suffix=_%s",
                UPDATE_APP_PATH, UPDATE_APP_NAME, shouldExecuteJar ? "y" : "n", Version.getCurrentVersion().toString());

        String command = String.format("java -jar %1$s %2$s", restarterAppPath, cmdArg);
        logger.info("Executing JAR of restarter with command: " + command);

        Process process = null;

        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            logger.error("Failed to run restarter.", e);
            return false;
        }

        if (!process.isAlive()) {
            logger.error("JAR restarter is not running.");
            return false;
        }
        return true;
    }

    private void promptUserToApplyUpdateImmediately() {
        Platform.runLater(() -> {
            String message = String.format("This will quit the application and restart it.%n" +
                    "Otherwise, update will be applied when you exit HubTurbo.");
            applyUpdateImmediately = DialogMessage.showYesNoWarningDialog("Update application",
                    "Would you like to update HubTurbo now?",
                    message,
                    "Yes", "No");
            if (applyUpdateImmediately && runJarUpdaterWithExecute()) {
                logger.info("Quitting application to apply update");
                ui.quit();
            }
        });
    }
}
