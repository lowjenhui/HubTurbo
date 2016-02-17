package tests;

import org.junit.Test;
import updater.UpdateDownloadLink;
import util.Version;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class UpdateDownloadLinkTest {

    @Test
    public void updateDownloadLinkCompareTo_sameVersionDiffLocation_sameObject() throws MalformedURLException {
        Version version = new Version(1, 0, 0);
        UpdateDownloadLink a = new UpdateDownloadLink();
        a.setVersion(version);
        a.setApplicationFileLocation(new URL("http://google.com"));

        UpdateDownloadLink b = new UpdateDownloadLink();
        b.setVersion(version);
        b.setApplicationFileLocation(new URL("http://yahoo.com"));

        assertTrue(a.compareTo(b) == 0);
        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void updateDownloadLinkCompareTo_diffVersionSameLocation_diffObject() throws MalformedURLException {
        String fileLocationString = "http://google.com";
        UpdateDownloadLink a = new UpdateDownloadLink();
        a.setVersion(new Version(2, 0, 0));
        a.setApplicationFileLocation(new URL(fileLocationString));

        UpdateDownloadLink b = new UpdateDownloadLink();
        b.setVersion(new Version(1, 2, 0));
        b.setApplicationFileLocation(new URL(fileLocationString));

        assertFalse(a.compareTo(b) == 0);
        assertFalse(a.equals(b));
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void updateDownloadLinkCompareTo_diffVersion_correctComparison() throws MalformedURLException {
        String fileLocationString = "http://google.com";
        UpdateDownloadLink a = new UpdateDownloadLink();
        a.setApplicationFileLocation(new URL(fileLocationString));

        UpdateDownloadLink b = new UpdateDownloadLink();
        b.setApplicationFileLocation(new URL(fileLocationString));

        a.setVersion(new Version(2, 0, 0));
        b.setVersion(new Version(1, 2, 0));
        assertTrue(a.compareTo(b) > 0);

        a.setVersion(new Version(1, 0, 0));
        b.setVersion(new Version(3, 0, 3));
        assertTrue(a.compareTo(b) < 0);
    }

    @Test
    public void updateDownloadLinkGetVersion_setVersionByReflection_getCorrectValue()
            throws NoSuchFieldException, IllegalAccessException, MalformedURLException {
        UpdateDownloadLink a = new UpdateDownloadLink();

        a.setApplicationFileLocation(new URL("http://google.com"));

        Version version = new Version(10, 11, 12);

        Class<?> updateDownloadLinkClass = a.getClass();

        Field versionField = updateDownloadLinkClass.getDeclaredField("version");
        versionField.setAccessible(true);

        versionField.set(a, version);

        assertEquals(version, a.getVersion());
    }

    @Test
    public void updateDownloadLinkGetFileLocation_setLocationByReflection_getCorrectValue()
            throws NoSuchFieldException, IllegalAccessException, MalformedURLException {
        UpdateDownloadLink a = new UpdateDownloadLink();
        a.setVersion(new Version(10, 11, 12));

        URL fileLocation = new URL("http://google.com");

        Class<?> updateDownloadLinkClass = a.getClass();

        Field applicationFileLocationField = updateDownloadLinkClass.getDeclaredField("applicationFileLocation");
        applicationFileLocationField.setAccessible(true);

        applicationFileLocationField.set(a, fileLocation);

        assertEquals(fileLocation, a.getApplicationFileLocation());
    }

    @Test
    public void updateDownloadLinkSetVersion_getVersionByReflection_valueSetCorrectly()
            throws NoSuchFieldException, IllegalAccessException, MalformedURLException {
        UpdateDownloadLink a = new UpdateDownloadLink();
        a.setApplicationFileLocation(new URL("http://google.com"));

        Version version = new Version(10, 11, 12);
        a.setVersion(version);

        Class<?> updateDownloadLinkClass = a.getClass();

        Field versionField = updateDownloadLinkClass.getDeclaredField("version");
        versionField.setAccessible(true);

        Version versionFromReflection = (Version) versionField.get(a);
        assertEquals(version, versionFromReflection);
    }

    @Test
    public void updateDownloadLinkSetFileLocation_getFileLocationByReflection_valueSetCorrectly()
            throws NoSuchFieldException, IllegalAccessException, MalformedURLException {
        UpdateDownloadLink a = new UpdateDownloadLink();
        a.setVersion(new Version(10, 11, 12));

        URL fileLocation = new URL("http://google.com");

        a.setApplicationFileLocation(fileLocation);

        Class<?> updateDownloadLinkClass = a.getClass();

        Field applicationFileLocationField = updateDownloadLinkClass.getDeclaredField("applicationFileLocation");
        applicationFileLocationField.setAccessible(true);

        URL fileLocationFromReflection = (URL) applicationFileLocationField.get(a);
        assertEquals(fileLocation, fileLocationFromReflection);
    }
}
