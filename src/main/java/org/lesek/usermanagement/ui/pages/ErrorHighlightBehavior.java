package org.lesek.usermanagement.ui.pages;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;

/**
 * Adds Bootstrap's {@code is-invalid} class to a form component's tag when it
 * failed validation, so the failing field is highlighted red instead of only
 * showing the error message in the feedback panel.
 */
public class ErrorHighlightBehavior extends Behavior {

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        if (component.hasErrorMessage()) {
            tag.append("class", "is-invalid", " ");
        }
    }
}
