package guitests;

import javafx.scene.input.KeyCode;
import org.junit.Test;
import ui.UI;
import ui.components.FilterTextField;
import util.events.UpdateProgressEvent;

import static org.junit.Assert.assertEquals;

public class UIComponentsTest extends UITest {
    
    // TODO check that filter text field does indeed do autocomplete correctly, etc
    @Test
    public void keywordCompletionTest() {
        click("#dummy/dummy_col0_filterTextField");
        selectAll();
        push(KeyCode.BACK_SPACE);
        type("ass");
        push(KeyCode.TAB);
        FilterTextField filterTextField = find("#dummy/dummy_col0_filterTextField");
        assertEquals("assignee", filterTextField.getText());
    }

    @Test
    public void filterTextFieldTest() {
        click("#dummy/dummy_col0_filterTextField");
        selectAll();
        push(KeyCode.BACK_SPACE);
        type("is:open OR is:closed");
        push(KeyCode.ENTER);
        
        FilterTextField filterTextField = find("#dummy/dummy_col0_filterTextField");
        filterTextField.clear();
        click("#dummy/dummy_col0_filterTextField");
        type("!@#$%^&*( ) { } :?");
        assertEquals("!@#$%^&*( ) { } :?", filterTextField.getText());
    }

    // TODO check that progress bar is updating
    @Test
    public void textProgressBarTest() {
        UI.events.triggerEvent(new UpdateProgressEvent("dummy/dummy", 0));
        sleep(1000);
        UI.events.triggerEvent(new UpdateProgressEvent("dummy/dummy", 0.5f));
        sleep(1000);
        UI.events.triggerEvent(new UpdateProgressEvent("dummy/dummy", 0.9f));
        sleep(1000);
        UI.events.triggerEvent(new UpdateProgressEvent("dummy/dummy"));
    }

}
